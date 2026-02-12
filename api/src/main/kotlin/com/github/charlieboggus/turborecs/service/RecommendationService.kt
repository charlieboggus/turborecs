package com.github.charlieboggus.turborecs.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.charlieboggus.turborecs.config.properties.ClaudeProperties
import com.github.charlieboggus.turborecs.db.entity.enums.MediaType
import com.github.charlieboggus.turborecs.db.repository.MediaItemRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

data class Recommendation(
    val title: String,
    val type: MediaType,
    val year: Int?,
    val creator: String?,
    val reason: String,
    val matchedThemes: List<String>
)

@Service
class RecommendationService(
    private val tasteProfileService: TasteProfileService,
    private val claudeApiService: ClaudeApiService,
    private val claudeProperties: ClaudeProperties,
    private val objectMapper: ObjectMapper,
    private val mediaItemRepository: MediaItemRepository
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
        require(count in 1..50) { "count must be between 1 and 50" }

        val profile = tasteProfileService.buildTasteProfile(modelVersion)

        // In your app, presence in media_items = already known; simplest and effective exclusion list.
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

        // Extra safety filters (Claude sometimes “forgets” rules)
        val alreadyKnownLower = alreadyKnownTitles.asSequence().map { it.trim().lowercase() }.toHashSet()
        val filtered = parsed
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

    private fun parseRecommendations(raw: String): List<Recommendation> {
        val cleaned = stripCodeFences(raw)

        val root = try {
            objectMapper.readTree(cleaned)
        } catch (e: Exception) {
            throw IllegalArgumentException("Claude returned invalid JSON for recommendations", e)
        }

        if (!root.isArray) {
            throw IllegalArgumentException("Claude recommendations response must be a JSON array")
        }

        val out = ArrayList<Recommendation>(root.size())
        for (node in root) {
            if (!node.isObject) continue

            val title = node.path("title").asText("").trim()
            val typeStr = node.path("type").asText("").trim()
            val type = runCatching { MediaType.valueOf(typeStr) }.getOrNull() ?: continue

            val year = node.path("year").let { y ->
                when {
                    y.isInt -> y.asInt()
                    y.isNumber -> y.asInt()
                    y.isTextual -> y.asText().toIntOrNull()
                    else -> null
                }
            }

            val creator = node.path("creator").takeIf { !it.isMissingNode && !it.isNull }?.asText(null)?.trim()
                ?.takeIf { it.isNotBlank() }

            val reason = node.path("reason").asText("").trim()
            if (title.isBlank() || reason.isBlank()) continue

            val matchedThemes = parseStringArray(node.get("matchedThemes"))
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .take(8)

            out += Recommendation(
                title = title,
                type = type,
                year = year,
                creator = creator,
                reason = reason,
                matchedThemes = matchedThemes
            )
        }

        return out
    }

    private fun parseStringArray(node: JsonNode?): List<String> {
        if (node == null || node.isNull) return emptyList()
        if (!node.isArray) return emptyList()
        val out = ArrayList<String>(node.size())
        for (x in node) {
            if (x.isTextual) out += x.asText()
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
}