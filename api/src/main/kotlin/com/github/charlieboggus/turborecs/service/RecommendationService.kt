package com.github.charlieboggus.turborecs.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.charlieboggus.turborecs.config.properties.ClaudeProperties
import com.github.charlieboggus.turborecs.db.entity.RecommendationLogEntity
import com.github.charlieboggus.turborecs.db.entity.enums.MediaType
import com.github.charlieboggus.turborecs.db.entity.enums.RecommendationSelection
import com.github.charlieboggus.turborecs.db.repository.MediaItemRepository
import com.github.charlieboggus.turborecs.db.repository.RecommendationLogRepository
import com.github.charlieboggus.turborecs.web.dto.RecommendationGridResponse
import com.github.charlieboggus.turborecs.web.dto.RecommendationTileResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.random.Random

data class Recommendation(
    val title: String,
    val type: MediaType,
    val year: Int?,
    val creator: String?,
    val reason: String,
    val matchedThemes: List<String>
)

private data class ParsedRecommendations(
    val items: List<Recommendation>,
    val partial: Boolean,           // true if parsing ended early (likely truncation)
    val parseError: String? = null  // optional: useful for logging
)

private val GRID_SIZE = 15
private val COOLDOWN_DAYS = 30L
private val GRID_STICKY_HOURS = 24L

@Service
class RecommendationService(
    private val tasteProfileService: TasteProfileService,
    private val claudeApiService: ClaudeApiService,
    private val claudeProperties: ClaudeProperties,
    private val objectMapper: ObjectMapper,
    private val mediaItemRepository: MediaItemRepository,
    private val recommendationLogRepository: RecommendationLogRepository
) {
    private val log = LoggerFactory.getLogger(RecommendationService::class.java)

    private val systemPrompt: String = """
        You are a media recommendation engine. Given a user's taste profile (themes, moods, 
        tones, and settings they enjoy, plus titles they loved and hated), recommend media 
        they would enjoy.

        Rules:
        - NEVER recommend anything from the topRatedTitles or lowRatedTitles lists
        - NEVER recommend anything in the "alreadyKnownTitles" list
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
        - matchedThemes: string array (3-5 specific tags from their profile this connects to)

        Return ONLY valid JSON. No markdown, no explanation, no backticks.
    """.trimIndent()

    fun recommend(
        count: Int = 10,
        mediaType: MediaType? = null,
        modelVersion: String = claudeProperties.model
    ): List<Recommendation> {
        require(count in 1..15) { "count must be between 1 and 15" }
        val profile = tasteProfileService.buildTasteProfile(modelVersion)
        val alreadyKnownTitles: List<String> = when (mediaType) {
            null -> mediaItemRepository.findAllTitles()
            else -> mediaItemRepository.findAllTitlesByMediaType(mediaType)
        }
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
            appendLine("alreadyKnownTitles that I have already watched/read (DO NOT recommend):")
            appendLine(alreadyKnownTitles.joinToString(", "))
            appendLine()
            appendLine(typeInstruction)
            appendLine("Recommend exactly $count items.")
        }
        val raw = claudeApiService.sendMessage(systemPrompt, userMessage)
        val parsed = parseRecommendations(raw)
        if (parsed.partial) {
            log.warn(
                "Claude recommendations parse was partial (likely truncation). parsedItems={}, requestedCount={}, err={}",
                parsed.items.size, count, parsed.parseError
            )
        }
        val alreadyKnownLower = alreadyKnownTitles.asSequence().map { it.trim().lowercase() }.toHashSet()
        val filtered = parsed.items
            .asSequence()
            .filter { it.title.isNotBlank() }
            .filter { it.title.trim().lowercase() !in alreadyKnownLower }
            .filter { mediaType == null || it.type == mediaType }
            .distinctBy { it.title.trim().lowercase() to it.type }
            .take(count)
            .toList()
        if (filtered.size < count) {
            log.warn(
                "RecommendationService returned {} items (requested {}), after filtering already-known titles",
                filtered.size, count
            )
        }
        return filtered
    }

    private fun formatScores(scores: Map<String, Double>): String {
        if (scores.isEmpty()) return "(none)"
        return scores.entries.joinToString(", ") { (k, v) -> "$k (${String.format("%.2f", v)})" }
    }

    private fun parseRecommendations(raw: String): ParsedRecommendations {
        val cleaned = stripCodeFences(raw).trim()
        // Fast fail if it doesn't even start like an array
        if (!cleaned.startsWith("[")) {
            throw IllegalArgumentException("Claude recommendations response must be a JSON array")
        }
        val parser = objectMapper.factory.createParser(cleaned)
        // Must start with array
        val first = parser.nextToken()
        if (first != JsonToken.START_ARRAY) {
            throw IllegalArgumentException("Claude recommendations response must be a JSON array")
        }
        val out = mutableListOf<Recommendation>()
        var partial = false
        var parseError: String? = null
        while (true) {
            val next = parser.nextToken()
            when (next) {
                null -> {
                    // truncated before END_ARRAY
                    partial = true
                    break
                }
                JsonToken.END_ARRAY -> break
                else -> {
                    try {
                        // read exactly one element from the array
                        val node: JsonNode = objectMapper.readTree(parser)
                        val rec = nodeToRecommendationOrNull(node)
                        if (rec != null) out += rec
                    }
                    catch (e: Exception) {
                        // likely truncated mid-object, stop and return what we have
                        partial = true
                        parseError = e.message
                        break
                    }
                }
            }
        }
        return ParsedRecommendations(items = out, partial = partial, parseError = parseError)
    }

    private fun nodeToRecommendationOrNull(node: JsonNode): Recommendation? {
        if (!node.isObject) {
            return null
        }
        val title = node.path("title").asText("").trim()
        val typeStr = node.path("type").asText("").trim()
        val type = runCatching { MediaType.valueOf(typeStr) }.getOrNull() ?: return null
        val year = node.path("year").let { y ->
            when {
                y.isInt -> y.asInt()
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
        val reason = node.path("reason").asText("").trim()
        if (title.isBlank() || reason.isBlank()) {
            return null
        }
        val matchedThemes = parseStringArray(node.get("matchedThemes"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .take(8)
        return Recommendation(
            title = title,
            type = type,
            year = year,
            creator = creator,
            reason = reason,
            matchedThemes = matchedThemes
        )
    }

    private fun parseStringArray(node: JsonNode?): List<String> {
        if (node == null || node.isNull) {
            return emptyList()
        }
        if (!node.isArray) {
            return emptyList()
        }
        val out = ArrayList<String>(node.size())
        for (x in node) {
            if (x.isTextual) {
                out += x.asText()
            }
        }
        return out
    }

    private fun stripCodeFences(raw: String): String {
        var s = raw.trim()
        if (s.startsWith("```")) {
            // remove leading fence line (``` or ```json etc.)
            val firstNewline = s.indexOf('\n')
            s = if (firstNewline >= 0) s.substring(firstNewline + 1) else s
        }
        if (s.endsWith("```")) {
            s = s.removeSuffix("```").trim()
        }
        return s.trim()
    }

    private fun norm(s: String): String =
        s.lowercase()
            .trim()
            .replace(Regex("""\s+"""), " ")
            .replace(Regex("""[^\p{L}\p{N}\s]"""), "")
            .replace(Regex("""^(the|a|an)\s+"""), "")

    private fun fingerprint(rec: Recommendation): String {
        val t = norm(rec.title)
        val c = rec.creator?.let { norm(it) }.orEmpty()
        val y = rec.year?.toString().orEmpty()
        return "${rec.type.name}|$t|$y|$c"
    }

    fun getOrCreateGrid(
        selection: RecommendationSelection,
        modelVersion: String = claudeProperties.model
    ): RecommendationGridResponse {
        val now = Instant.now()
        val since = now.minus(Duration.ofHours(GRID_STICKY_HOURS))
        val recentBatchId = recommendationLogRepository
            .findRecentBatchIds(modelVersion, selection, since)
            .firstOrNull()
        if (recentBatchId != null) {
            val rows = recommendationLogRepository.findByBatchIdAndReplacedByIsNullOrderBySlot(recentBatchId)
            if (rows.size == GRID_SIZE) {
                return toGridResponse(selection, recentBatchId, rows)
            }
            // If partial/corrupt, fall through and regenerate
        }
        return generateNewGrid(selection, modelVersion)
    }

    @Transactional
    fun generateNewGrid(
        selection: RecommendationSelection,
        modelVersion: String = claudeProperties.model
    ): RecommendationGridResponse {
        val now = Instant.now()
        val batchId = UUID.randomUUID()
        val active = recommendationLogRepository.findActiveFingerprints(modelVersion, now).toHashSet()
        val items = generateDistinctRecommendations(
            selection = selection,
            count = GRID_SIZE,
            modelVersion = modelVersion,
            activeFingerprints = active
        )
        val expiresAt = now.plus(Duration.ofDays(COOLDOWN_DAYS))
        val rows = items.mapIndexed { idx, r ->
            RecommendationLogEntity(
                id = UUID.randomUUID(),
                modelVersion = modelVersion,
                selection = selection,
                mediaType = r.type,
                title = r.title,
                year = r.year,
                creator = r.creator,
                reason = r.reason,
                matchedThemes = r.matchedThemes.toMutableList(),
                fingerprint = fingerprint(r),
                batchId = batchId,
                slot = idx,
                shownAt = now,
                expiresAt = expiresAt,
                replacedBy = null
            )
        }
        recommendationLogRepository.saveAll(rows)
        return toGridResponse(selection, batchId, rows)
    }

    @Transactional
    fun refreshSlot(
        batchId: UUID,
        slot: Int,
        selection: RecommendationSelection,
        modelVersion: String = claudeProperties.model
    ): RecommendationTileResponse {
        require(slot in 0 until GRID_SIZE) { "slot must be between 0 and ${GRID_SIZE - 1}" }
        val now = Instant.now()

        val existing = recommendationLogRepository.findByBatchIdAndReplacedByIsNullOrderBySlot(batchId)
        val current = recommendationLogRepository.findByBatchIdAndSlotAndReplacedByIsNull(batchId, slot)
            ?: throw NoSuchElementException("No active tile at slot=$slot for batchId=$batchId")
        val active = recommendationLogRepository.findActiveFingerprints(modelVersion, now).toHashSet()
        existing.forEach { active += it.fingerprint }

        val replacementType = when (selection) {
            RecommendationSelection.BOOKS -> MediaType.BOOK
            RecommendationSelection.MOVIES -> MediaType.MOVIE
            RecommendationSelection.BOTH -> if (Random.nextBoolean()) MediaType.MOVIE else MediaType.BOOK
        }
        val replacement = generateDistinctRecommendations(
            selection = selection,
            count = 1,
            modelVersion = modelVersion,
            activeFingerprints = active,
            forceType = replacementType
        ).firstOrNull() ?: throw IllegalStateException("Failed to generate replacement recommendation")
        val expiresAt = now.plus(Duration.ofDays(COOLDOWN_DAYS))
        val newRow = RecommendationLogEntity(
            id = UUID.randomUUID(),
            modelVersion = modelVersion,
            selection = selection,
            mediaType = replacement.type,
            title = replacement.title,
            year = replacement.year,
            creator = replacement.creator,
            reason = replacement.reason,
            matchedThemes = replacement.matchedThemes.toMutableList(),
            fingerprint = fingerprint(replacement),
            batchId = batchId,
            slot = slot,
            shownAt = now,
            expiresAt = expiresAt
        )
        recommendationLogRepository.save(newRow)
        current.replacedBy = newRow.id
        recommendationLogRepository.save(current)
        return RecommendationTileResponse(
            slot = slot,
            id = requireNotNull(newRow.id),
            title = newRow.title,
            type = newRow.mediaType,
            year = newRow.year,
            creator = newRow.creator,
            reason = newRow.reason,
            matchedThemes = newRow.matchedThemes.toList()
        )
    }

    private fun toGridResponse(
        selection: RecommendationSelection,
        batchId: UUID,
        rows: List<RecommendationLogEntity>
    ): RecommendationGridResponse =
        RecommendationGridResponse(
            batchId = batchId,
            selection = selection,
            items = rows.sortedBy { it.slot }.map {
                RecommendationTileResponse(
                    slot = it.slot,
                    id = requireNotNull(it.id),
                    title = it.title,
                    type = it.mediaType,
                    year = it.year,
                    creator = it.creator,
                    reason = it.reason,
                    matchedThemes = it.matchedThemes.toList()
                )
            }
        )

    private fun generateDistinctRecommendations(
        selection: RecommendationSelection,
        count: Int,
        modelVersion: String,
        activeFingerprints: MutableSet<String>,
        forceType: MediaType? = null
    ): List<Recommendation> {
        val out = mutableListOf<Recommendation>()
        var attempts = 0
        while (out.size < count && attempts < 6) {
            attempts++
            val typeParam: MediaType? = when {
                forceType != null -> forceType
                selection == RecommendationSelection.BOOKS -> MediaType.BOOK
                selection == RecommendationSelection.MOVIES -> MediaType.MOVIE
                else -> null // BOTH
            }
            val batch = recommend(
                count = minOf(15, count - out.size),
                mediaType = typeParam,
                modelVersion = modelVersion
            )
            for (r in batch) {
                val fp = fingerprint(r)
                if (fp in activeFingerprints) {
                    continue
                }
                activeFingerprints += fp
                out += r
                if (out.size == count) {
                    break
                }
            }
        }
        return out
    }
}