package com.github.charlieboggus.turborecs.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.charlieboggus.turborecs.client.ClaudeClient
import com.github.charlieboggus.turborecs.common.enums.TagCategory
import com.github.charlieboggus.turborecs.common.enums.TaggingStatus
import com.github.charlieboggus.turborecs.config.properties.ClaudeProperties
import com.github.charlieboggus.turborecs.db.entities.MediaItemEntity
import com.github.charlieboggus.turborecs.db.entities.MediaTagEntity
import com.github.charlieboggus.turborecs.db.entities.TagEntity
import com.github.charlieboggus.turborecs.db.repository.MediaTagRepository
import com.github.charlieboggus.turborecs.db.repository.TagRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import java.time.Instant
import kotlin.math.max
import kotlin.math.min

@Service
class TaggingService(
    private val claudeProperties: ClaudeProperties,
    private val claudeClient: ClaudeClient,
    private val tagRepository: TagRepository,
    private val mediaTagRepository: MediaTagRepository,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(TaggingService::class.java)

    private val systemPrompt: String = """
        You are a media analysis assistant. Given a movie or book title and its media type,
        generate thematic tags with weighted scores.

        Return ONLY a JSON object with exactly these four keys: THEME, MOOD, TONE, SETTING.
        Each key maps to an object of tag names to weights (0.0 to 1.0).

        Guidelines:
        - THEME: core thematic elements (e.g., "obsessive ambition", "identity", "corruption of power")
        - MOOD: emotional atmosphere (e.g., "dread", "whimsical", "melancholic")
        - TONE: stylistic quality (e.g., "dark", "satirical", "epic", "absurdist")
        - SETTING: time/place/world (e.g., "1970s Los Angeles", "deep space", "medieval fantasy")
        - Use 3-6 tags per category
        - Weights reflect how central the tag is (0.9+ = defining, 0.5-0.7 = present but secondary)
        - Return ONLY valid JSON. No markdown, no backticks.
    """.trimIndent()

    fun tagItem(item: MediaItemEntity) {
        val userMessage = buildString {
            appendLine("Title: \"${item.title}\"")
            appendLine("Media type: ${item.mediaType}")
        }

        val raw = claudeClient.sendMessage(systemPrompt, userMessage)

        val parsed = try {
            parseClaudeTagJson(raw)
        }
        catch (e: Exception) {
            log.warn(
                "Claude tag parsing failed for mediaId={} modelVersion={}. rawPreview='{}' err={}",
                item.id,
                claudeProperties.model,
                raw.take(300).replace("\n", "\\n"),
                e.message
            )
            item.taggingStatus = TaggingStatus.FAILED
            throw e
        }

        val now = Instant.now()
        val replacements = buildMediaTags(item, parsed, now, claudeProperties.model)

        if (replacements.isEmpty()) {
            log.warn(
                "Claude returned no usable tags for mediaId={} modelVersion={}",
                item.id,
                claudeProperties.model
            )
            item.taggingStatus = TaggingStatus.FAILED
            return
        }

        // Wipe existing tags for this item before saving new ones to avoid unique constraint violations
        mediaTagRepository.deleteAllByMediaItemId(item.id)
        mediaTagRepository.saveAll(replacements)

        item.taggingStatus = TaggingStatus.TAGGED

        log.info(
            "Tagged mediaId={} with {} tags (modelVersion={})",
            item.id,
            replacements.size,
            claudeProperties.model
        )
    }

    private fun buildMediaTags(
        item: MediaItemEntity,
        parsed: Map<TagCategory, Map<String, Double>>,
        now: Instant,
        modelVersion: String
    ): List<MediaTagEntity> {
        val deduped: MutableMap<Pair<TagCategory, String>, Double> = linkedMapOf()

        for ((category, tagWeight) in parsed) {
            for ((rawName, rawWeight) in tagWeight) {
                val name = normalizeTagName(rawName)
                if (name.isEmpty()) {
                    continue
                }

                val weight = sanitizeTagWeight(rawWeight)
                if (weight <= 0.0) {
                    continue
                }

                val key = category to name
                deduped[key] = max(deduped[key] ?: 0.0, weight)
            }
        }

        if (deduped.isEmpty()) {
            return emptyList()
        }

        val tagsByKey: MutableMap<Pair<TagCategory, String>, TagEntity> = linkedMapOf()
        for ((key, _) in deduped) {
            val (category, name) = key
            tagsByKey[key] = findOrCreateTag(category, name)
        }

        return deduped.entries.map { (key, weight) ->
            MediaTagEntity(
                mediaItem = item,
                tag = tagsByKey.getValue(key),
                weight = weight,
                modelVersion = modelVersion,
                generatedAt = now
            )
        }
    }

    private fun findOrCreateTag(category: TagCategory, normalizedName: String): TagEntity {
        tagRepository.findByCategoryAndName(category, normalizedName)?.let { return it }
        return try {
            tagRepository.save(
                TagEntity(
                    name = normalizedName,
                    category = category
                )
            )
        }
        catch (e: DataIntegrityViolationException) {
            tagRepository.findByCategoryAndName(category, normalizedName) ?: throw e
        }
    }

    private fun parseClaudeTagJson(raw: String): Map<TagCategory, Map<String, Double>> {
        val cleaned = stripCodeFences(raw)
        val root = try {
            objectMapper.readTree(cleaned)
        }
        catch (e: Exception) {
            throw IllegalArgumentException("Claude returned invalid JSON", e)
        }
        if (!root.isObject) {
            throw IllegalArgumentException("Claude response must be a JSON object")
        }
        return linkedMapOf(
            TagCategory.THEME to parseCategory(root, "THEME"),
            TagCategory.MOOD to parseCategory(root, "MOOD"),
            TagCategory.TONE to parseCategory(root, "TONE"),
            TagCategory.SETTING to parseCategory(root, "SETTING"),
        )
    }

    private fun parseCategory(root: JsonNode, key: String): Map<String, Double> {
        val node = root.get(key)
            ?: throw IllegalArgumentException("Claude response missing required key '$key'")

        if (!node.isObject) {
            throw IllegalArgumentException("Claude response key '$key' must map to {tagName: weight}")
        }

        val out = LinkedHashMap<String, Double>()
        for ((name, weightNode) in node.properties()) {
            val w = when {
                weightNode.isNumber -> weightNode.asDouble()
                weightNode.isTextual -> weightNode.asText().toDoubleOrNull()
                else -> null
            }
            if (w != null) {
                out[name] = w
            }
        }
        return out
    }

    private fun stripCodeFences(raw: String): String {
        val s = raw.trim()
        if (!s.startsWith("```")) {
            return s
        }
        return s
            .removePrefix("```json")
            .removePrefix("```")
            .trimStart()
            .removeSuffix("```")
            .trim()
    }

    private fun normalizeTagName(name: String): String {
        return name.trim()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("""^[\p{Punct}\s]+|[\p{Punct}\s]+$"""), "")
            .lowercase()
    }

    private fun sanitizeTagWeight(raw: Double): Double {
        if (!raw.isFinite()) return 0.0
        return min(1.0, max(0.0, raw))
    }
}