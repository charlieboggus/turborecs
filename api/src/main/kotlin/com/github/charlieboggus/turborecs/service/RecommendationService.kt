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

data class Recommendation(
    val title: String,
    val mediaType: MediaType,
    val year: Int?,
    val creator: String?,
    val reason: String,
    val matchedTags: List<String>
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
        You are a media recommendation engine. Given a user's taste profile (themes, moods, 
        tones, and settings they enjoy, plus titles they loved and hated), recommend media 
        they would enjoy.

        Rules:
        - NEVER recommend anything from the topRatedTitles or lowRatedTitles lists
        - NEVER recommend anything in the "alreadyKnownTitles" or "excludedTitles" lists
        - Recommend things the user likely hasn't encountered — avoid the most obvious picks
        - For cross-media recommendations, find thematic connections, not surface-level genre matches
        - Don't recommend sequels unless the user has seen/read the preceding entries
        - Each recommendation should connect to multiple elements of their taste profile

        Return ONLY a JSON array of objects with these fields:
        - title: string
        - type: "MOVIE" or "BOOK"
        - year: number|null
        - creator: string|null
        - reason: string (2-3 sentences, reference specific profile tags)
        - matchedTags: string array (3-5 specific tags from their profile this connects to)

        Return ONLY valid JSON. No markdown, no explanation, no backticks.
    """.trimIndent()

    /**
     * Returns the current cached grid if it exists and is fresh,
     * otherwise generates a new one.
     */
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

    /**
     * Forces generation of a brand-new grid, ignoring any cache.
     */
    @Transactional
    fun forceRefreshGrid(mediaType: MediaType? = null): RecommendationGridResponse {
        return generateGrid(mediaType)
    }

    @Transactional
    fun getCachedGrid(mediaType: MediaType? = null): RecommendationGridResponse? {
        val active = recommendationLogRepository.findActiveRecommendations(Instant.now())
        if (active.isEmpty()) {
            return null
        }
        val items = if (mediaType != null) {
            active.filter { it.mediaType == mediaType }
        }
        else {
            active
        }
        val batchId = items.first().batchId
        return toGridResponse(batchId, items)
    }

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
            RecommendationLogEntity(
                batchId = batchId,
                modelVersion = modelVersion,
                mediaType = rec.mediaType,
                title = rec.title,
                year = rec.year,
                creator = rec.creator,
                reason = rec.reason,
                matchedTags = rec.matchedTags,
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
            if (collected.size >= count) {
                break
            }
            val remaining = count - collected.size
            val batch = callClaude(
                mediaType = mediaType,
                count = minOf(15, remaining + 3), // request a few extra to account for dedup filtering
            )
            for (rec in batch) {
                val fp = fingerprint(rec)
                if (fp in activeFingerprints) {
                    continue
                }
                activeFingerprints += fp
                collected += rec
                if (collected.size >= count) {
                    break
                }
            }
            if (batch.isEmpty()) {
                log.warn("Claude returned 0 recommendations on attempt {}", attempt)
                break
            }
        }
        if (collected.size < count) {
            log.warn("Only generated {} of {} requested recommendations after {} attempts",
                collected.size, count, MAX_GENERATION_ATTEMPTS)
        }
        return collected
    }

    private fun callClaude(
        mediaType: MediaType?,
        count: Int,
    ): List<Recommendation> {
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
            appendLine("Here is my taste profile:")
            appendLine()
            appendLine("Top themes: ${formatScores(profile.themes)}")
            appendLine("Top moods: ${formatScores(profile.moods)}")
            appendLine("Top tones: ${formatScores(profile.tones)}")
            appendLine("Top settings: ${formatScores(profile.settings)}")
            appendLine()
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
                    }
                    catch (e: Exception) {
                        truncated = true
                        log.debug("Stopped parsing recommendations mid-array: {}", e.message)
                        break
                    }
                }
            }
        }
        if (truncated) {
            log.warn("Claude recommendations were truncated. Parsed {} items before cutoff.", results.size)
        }
        return results
    }

    private fun nodeToRecommendation(node: JsonNode): Recommendation? {
        if (!node.isObject) {
            return null
        }
        val title = node.path("title").asText("").trim()
        val reason = node.path("reason").asText("").trim()
        if (title.isBlank() || reason.isBlank()) {
            return null
        }
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
            ?.asText(null)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        val matchedTags = node.path("matchedTags").let { arr ->
            if (arr.isArray) arr.mapNotNull { it.asText(null)?.trim()?.takeIf(String::isNotBlank) }.take(8)
            else emptyList()
        }
        return Recommendation(
            title = title,
            mediaType = mediaType,
            year = year,
            creator = creator,
            reason = reason,
            matchedTags = matchedTags
        )
    }

    private fun toGridResponse(
        batchId: UUID,
        rows: List<RecommendationLogEntity>
    ) = RecommendationGridResponse(
        batchId = batchId,
        items = rows.map { RecommendationResponse.from(it) }
    )

    private fun fingerprint(rec: Recommendation): String {
        val t = normalize(rec.title)
        val c = rec.creator?.let { normalize(it) }.orEmpty()
        val y = rec.year?.toString().orEmpty()
        return "${rec.mediaType.name}|$t|$y|$c"
    }

    private fun normalize(s: String): String =
        s.lowercase().trim()
            .replace(Regex("""\s+"""), " ")
            .replace(Regex("""[^\p{L}\p{N}\s]"""), "")
            .replace(Regex("""^(the|a|an)\s+"""), "")

    private fun formatScores(scores: Map<String, Double>): String {
        if (scores.isEmpty()) {
            return "(none)"
        }
        return scores.entries.joinToString(", ") { (k, v) -> "$k (${String.format("%.2f", v)})" }
    }

    private fun stripCodeFences(raw: String): String {
        var s = raw.trim()
        if (s.startsWith("```")) {
            val firstNewline = s.indexOf('\n')
            s = if (firstNewline >= 0) s.substring(firstNewline + 1) else s
        }
        if (s.endsWith("```")) {
            s = s.removeSuffix("```").trim()
        }
        return s.trim()
    }
}