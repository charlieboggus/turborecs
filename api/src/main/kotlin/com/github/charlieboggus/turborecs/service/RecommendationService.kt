package com.github.charlieboggus.turborecs.service

import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.charlieboggus.turborecs.client.ClaudeClient
import com.github.charlieboggus.turborecs.common.enums.MediaType
import com.github.charlieboggus.turborecs.config.properties.ClaudeProperties
import com.github.charlieboggus.turborecs.db.entities.RecommendationLogEntity
import com.github.charlieboggus.turborecs.db.repository.ExclusionRepository
import com.github.charlieboggus.turborecs.db.repository.MediaItemRepository
import com.github.charlieboggus.turborecs.db.repository.RecommendationLogRepository
import com.github.charlieboggus.turborecs.dto.response.RecommendationGridResponse
import com.github.charlieboggus.turborecs.dto.response.RecommendationResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.UUID

data class MatchedDimension(
    val dimension: String,
    val userScore: Double,
    val estimatedItemScore: Double,
    val matchStrength: String // "strong", "moderate", "contrast"
)

data class Recommendation(
    val title: String,
    val mediaType: MediaType,
    val year: Int?,
    val creator: String?,
    val reason: String,
    val matchedTags: List<String>,
    val matchedDimensions: List<MatchedDimension>
)

@Service
class RecommendationService(
    private val tasteProfileService: TasteProfileService,
    private val claudeClient: ClaudeClient,
    private val claudeProperties: ClaudeProperties,
    private val objectMapper: ObjectMapper,
    private val mediaItemRepository: MediaItemRepository,
    private val exclusionRepository: ExclusionRepository,
    private val recommendationLogRepository: RecommendationLogRepository
) {
    private val log = LoggerFactory.getLogger(RecommendationService::class.java)

    companion object {
        private const val GRID_SIZE = 15
        private const val COOLDOWN_DAYS = 30L
        private const val CACHE_HOURS = 24L
        private const val MAX_GENERATION_ATTEMPTS = 4
    }

    private val systemPrompt: String = """
        You are a media recommendation engine that uses a 12-dimension taste vector to find precise matches.
        
        DIMENSION RUBRIC (each 0.0–1.0):
        EMOTIONAL_INTENSITY:   0=detached/cerebral, 1=devastating/gut-punch
        NARRATIVE_COMPLEXITY:  0=linear, 1=non-linear/multi-threaded
        MORAL_AMBIGUITY:       0=clear heroes/villains, 1=gray zones
        TONE_DARKNESS:         0=light/optimistic, 1=bleak/nihilistic
        PACING:                0=slow-burn/meditative, 1=relentless/propulsive
        HUMOR:                 0=dead serious, 1=pervasively funny
        VIOLENCE_INTENSITY:    0=gentle, 1=graphic/brutal
        INTELLECTUAL_DEPTH:    0=escapism, 1=ideas-driven
        STYLISTIC_BOLDNESS:    0=conventional, 1=experimental/auteur
        INTIMACY_SCALE:        0=epic scope, 1=claustrophobic/personal
        REALISM:               0=fantastical/surreal, 1=grounded/naturalistic
        CULTURAL_SPECIFICITY:  0=universal, 1=deep in specific place/era

        MATCHING STRATEGY:
        - Primary: match the user's tasteVector (high dimensions they like)
        - Avoid: dimensions prominent in their antiVector (things they dislike)
        - Supplemental: use tag preferences and loved/hated titles for flavor
        - Cross-media: find thematic connections, not surface-level genre matches
        - Avoid obvious picks; recommend things the user likely hasn't encountered

        RULES:
        - NEVER recommend anything from alreadyKnownTitles or excludedTitles
        - Don't recommend sequels unless user has seen/read predecessors
        - Each recommendation must connect to 2+ strong dimension matches

        Return ONLY a JSON array. Each object must have:
        {
          "title": "string",
          "type": "MOVIE" or "BOOK",
          "year": number|null,
          "creator": "string|null",
          "reason": "2-3 sentences referencing specific dimensions and tags",
          "matchedTags": ["tag1","tag2","tag3"],
          "matchedDimensions": [
            {"dimension":"TONE_DARKNESS","userScore":0.82,"estimatedItemScore":0.85,"matchStrength":"strong"},
            {"dimension":"MORAL_AMBIGUITY","userScore":0.78,"estimatedItemScore":0.90,"matchStrength":"strong"}
          ]
        }

        matchStrength: "strong" (scores within 0.15), "moderate" (within 0.30), "contrast" (deliberate mismatch for variety).
        Include 2-4 matchedDimensions per recommendation.

        Self-check: validate JSON, ensure count == N, never include excluded titles.
        Return ONLY valid JSON. No markdown, no explanation, no backticks.
    """.trimIndent()

    // ── Public API ──────────────────────────────────────────────────────────

    @Transactional
    fun getOrRefreshGrid(mediaType: MediaType? = null): RecommendationGridResponse {
        val modelVersion = claudeProperties.model
        val now = Instant.now()
        val since = now.minus(Duration.ofHours(CACHE_HOURS))
        val cachedBatchId = recommendationLogRepository
            .findRecentBatchIds(modelVersion, since)
            .firstOrNull()
        if (cachedBatchId != null) {
            val rows = recommendationLogRepository.findAllByBatchId(cachedBatchId)
            if (rows.size == GRID_SIZE) {
                log.info("Returning cached grid batchId={} ({} items)", cachedBatchId, rows.size)
                return toGridResponse(cachedBatchId, rows)
            }
        }
        return generateGrid(mediaType)
    }

    @Transactional
    fun forceRefreshGrid(mediaType: MediaType? = null): RecommendationGridResponse {
        return generateGrid(mediaType)
    }

    @Transactional(readOnly = true)
    fun getCachedGrid(mediaType: MediaType? = null): RecommendationGridResponse? {
        val active = recommendationLogRepository.findActiveRecommendations(Instant.now())
        if (active.isEmpty()) return null
        val items = if (mediaType != null) active.filter { it.mediaType == mediaType } else active
        if (items.isEmpty()) return null
        return toGridResponse(items.first().batchId, items)
    }

    // ── Generation ──────────────────────────────────────────────────────────

    private fun generateGrid(mediaType: MediaType?): RecommendationGridResponse {
        val modelVersion = claudeProperties.model
        val now = Instant.now()
        val batchId = UUID.randomUUID()
        val activeFingerprints = recommendationLogRepository
            .findActiveFingerprints(modelVersion, now)
            .toMutableSet()

        val items = generateDistinctRecommendations(
            mediaType = mediaType,
            count = GRID_SIZE,
            activeFingerprints = activeFingerprints
        )

        val expiresAt = now.plus(Duration.ofDays(COOLDOWN_DAYS))
        val rows = items.map { rec ->
            val dimJson = try {
                objectMapper.writeValueAsString(rec.matchedDimensions)
            } catch (_: Exception) { null }

            RecommendationLogEntity(
                batchId = batchId,
                modelVersion = modelVersion,
                mediaType = rec.mediaType,
                title = rec.title,
                year = rec.year,
                creator = rec.creator,
                reason = rec.reason,
                matchedTags = rec.matchedTags,
                matchedDimensions = dimJson,
                fingerprint = fingerprint(rec),
                shownAt = now,
                expiresAt = expiresAt
            )
        }
        recommendationLogRepository.saveAll(rows)
        log.info("Generated new grid batchId={} with {} items", batchId, rows.size)
        return toGridResponse(batchId, rows)
    }

    private fun generateDistinctRecommendations(
        mediaType: MediaType?,
        count: Int,
        activeFingerprints: MutableSet<String>
    ): List<Recommendation> {
        val collected = mutableListOf<Recommendation>()
        for (attempt in 1..MAX_GENERATION_ATTEMPTS) {
            if (collected.size >= count) break
            val remaining = count - collected.size
            val batch = callClaude(
                mediaType = mediaType,
                count = minOf(15, remaining + 3)
            )
            for (rec in batch) {
                val fp = fingerprint(rec)
                if (fp in activeFingerprints) continue
                activeFingerprints += fp
                collected += rec
                if (collected.size >= count) break
            }
            if (batch.isEmpty()) {
                log.warn("Claude returned 0 recommendations on attempt {}", attempt)
                break
            }
        }
        if (collected.size < count) {
            log.warn("Only generated {} of {} requested after {} attempts",
                collected.size, count, MAX_GENERATION_ATTEMPTS)
        }
        return collected
    }

    // ── Claude Call ──────────────────────────────────────────────────────────

    private fun callClaude(mediaType: MediaType?, count: Int): List<Recommendation> {
        val profile = tasteProfileService.buildTasteProfile()

        val libraryTitles = mediaItemRepository.findAll()
            .map { it.title.trim().lowercase() }
            .toSet()

        val excludedTitles = exclusionRepository.findAll()
            .map { it.title.trim().lowercase() }
            .toSet()

        val typeInstruction = when (mediaType) {
            MediaType.MOVIE -> "Recommend ONLY movies."
            MediaType.BOOK -> "Recommend ONLY books."
            null -> "Recommend a mix of movies and books."
        }

        val userMessage = buildString {
            // Primary signal: dimension vectors
            if (profile.tasteVector.isNotEmpty()) {
                appendLine("=== TASTE VECTOR (primary matching signal) ===")
                appendLine(formatVector(profile.tasteVector))
                appendLine()
                if (profile.antiVector.any { it.value > 0.0 }) {
                    appendLine("=== ANTI-VECTOR (dimensions to avoid) ===")
                    appendLine(formatVector(profile.antiVector))
                    appendLine()
                }
            }

            // Supplemental signal: tags
            if (profile.themes.isNotEmpty() || profile.moods.isNotEmpty()) {
                appendLine("=== TAG PREFERENCES (supplemental flavor) ===")
                if (profile.themes.isNotEmpty()) appendLine("Themes: ${formatScores(profile.themes)}")
                if (profile.moods.isNotEmpty()) appendLine("Moods: ${formatScores(profile.moods)}")
                if (profile.tones.isNotEmpty()) appendLine("Tones: ${formatScores(profile.tones)}")
                if (profile.settings.isNotEmpty()) appendLine("Settings: ${formatScores(profile.settings)}")
                appendLine()
            }

            appendLine("Titles I loved (4-5 stars): ${profile.topRatedTitles.joinToString(", ")}")
            appendLine("Titles I disliked (1-2 stars): ${profile.lowRatedTitles.joinToString(", ")}")
            appendLine()
            appendLine("alreadyKnownTitles (DO NOT recommend): ${libraryTitles.joinToString(", ")}")
            appendLine("excludedTitles (DO NOT recommend): ${excludedTitles.joinToString(", ")}")
            appendLine()
            appendLine(typeInstruction)
            appendLine("Recommend exactly $count items.")
        }

        val raw = claudeClient.sendMessage(systemPrompt, userMessage)
        return parseAndFilter(raw, mediaType, libraryTitles + excludedTitles)
    }

    // ── Parsing ─────────────────────────────────────────────────────────────

    private fun parseAndFilter(
        raw: String,
        mediaType: MediaType?,
        knownTitlesLower: Set<String>
    ): List<Recommendation> {
        val parsed = parseRecommendationsArray(raw)
        return parsed
            .filter { it.title.isNotBlank() }
            .filter { it.title.trim().lowercase() !in knownTitlesLower }
            .filter { mediaType == null || it.mediaType == mediaType }
            .distinctBy { it.title.trim().lowercase() to it.mediaType }
    }

    private fun parseRecommendationsArray(raw: String): List<Recommendation> {
        val cleaned = stripCodeFences(raw).trim()
        if (!cleaned.startsWith("[")) {
            throw IllegalArgumentException("Claude recommendations response must be a JSON array")
        }
        val parser = objectMapper.factory.createParser(cleaned)
        val firstToken = parser.nextToken()
        if (firstToken != JsonToken.START_ARRAY) {
            throw IllegalArgumentException("Claude recommendations response must be a JSON array")
        }
        val results = mutableListOf<Recommendation>()
        var truncated = false
        while (true) {
            val token = parser.nextToken()
            when (token) {
                null -> { truncated = true; break }
                JsonToken.END_ARRAY -> break
                else -> {
                    try {
                        val node: JsonNode = objectMapper.readTree(parser)
                        nodeToRecommendation(node)?.let { results += it }
                    } catch (e: Exception) {
                        truncated = true
                        log.debug("Stopped parsing mid-array: {}", e.message)
                        break
                    }
                }
            }
        }
        if (truncated) {
            log.warn("Claude recommendations truncated. Parsed {} items.", results.size)
        }
        return results
    }

    private fun nodeToRecommendation(node: JsonNode): Recommendation? {
        if (!node.isObject) return null

        val title = node.path("title").asText("").trim()
        val reason = node.path("reason").asText("").trim()
        if (title.isBlank() || reason.isBlank()) return null

        val typeStr = node.path("type").asText("").trim()
        val mediaType = runCatching { MediaType.valueOf(typeStr) }.getOrNull() ?: return null

        val year = node.path("year").let { y ->
            when {
                y.isNumber -> y.asInt()
                y.isTextual -> y.asText().toIntOrNull()
                else -> null
            }
        }

        val creator = node.path("creator")
            .takeIf { !it.isMissingNode && !it.isNull }
            ?.asText(null)?.trim()?.takeIf { it.isNotBlank() }

        val matchedTags = node.path("matchedTags").let { arr ->
            if (arr.isArray) arr.mapNotNull { it.asText(null)?.trim()?.takeIf(String::isNotBlank) }.take(8)
            else emptyList()
        }

        val matchedDimensions = node.path("matchedDimensions").let { arr ->
            if (arr.isArray) {
                arr.mapNotNull { dimNode ->
                    try {
                        MatchedDimension(
                            dimension = dimNode.path("dimension").asText(""),
                            userScore = dimNode.path("userScore").asDouble(0.0),
                            estimatedItemScore = dimNode.path("estimatedItemScore").asDouble(0.0),
                            matchStrength = dimNode.path("matchStrength").asText("moderate")
                        )
                    } catch (_: Exception) { null }
                }.take(6)
            } else emptyList()
        }

        return Recommendation(
            title = title,
            mediaType = mediaType,
            year = year,
            creator = creator,
            reason = reason,
            matchedTags = matchedTags,
            matchedDimensions = matchedDimensions
        )
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun toGridResponse(batchId: UUID, rows: List<RecommendationLogEntity>) =
        RecommendationGridResponse(
            batchId = batchId,
            items = rows.map { RecommendationResponse.from(it) }
        )

    private fun fingerprint(rec: Recommendation): String {
        val t = normalizeFp(rec.title)
        val c = rec.creator?.let { normalizeFp(it) }.orEmpty()
        val y = rec.year?.toString().orEmpty()
        return "${rec.mediaType.name}|$t|$y|$c"
    }

    private fun normalizeFp(s: String): String =
        s.lowercase().trim()
            .replace(Regex("""\s+"""), " ")
            .replace(Regex("""[^\p{L}\p{N}\s]"""), "")
            .replace(Regex("""^(the|a|an)\s+"""), "")

    private fun formatVector(vec: Map<String, Double>): String =
        vec.entries.joinToString(", ") { (k, v) -> "$k=${String.format("%.2f", v)}" }

    private fun formatScores(scores: Map<String, Double>): String {
        if (scores.isEmpty()) return "(none)"
        return scores.entries.take(8).joinToString(", ") { (k, v) -> "$k (${String.format("%.2f", v)})" }
    }

    private fun stripCodeFences(raw: String): String {
        var s = raw.trim()
        if (s.startsWith("```")) {
            val firstNewline = s.indexOf('\n')
            s = if (firstNewline >= 0) s.substring(firstNewline + 1) else s
        }
        if (s.endsWith("```")) s = s.removeSuffix("```").trim()
        return s.trim()
    }
}