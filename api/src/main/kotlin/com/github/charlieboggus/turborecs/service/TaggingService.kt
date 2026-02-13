package com.github.charlieboggus.turborecs.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.charlieboggus.turborecs.config.properties.ClaudeProperties
import com.github.charlieboggus.turborecs.db.entity.MediaTagEntity
import com.github.charlieboggus.turborecs.db.entity.TagEntity
import com.github.charlieboggus.turborecs.db.entity.enums.TagCategory
import com.github.charlieboggus.turborecs.db.repository.MediaItemRepository
import com.github.charlieboggus.turborecs.db.repository.MediaTagRepository
import com.github.charlieboggus.turborecs.db.repository.TagRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

@Service
class TaggingService(
    private val claudeApiService: ClaudeApiService,
    private val claudeProperties: ClaudeProperties,
    private val mediaItemRepository: MediaItemRepository,
    private val tagRepository: TagRepository,
    private val mediaTagRepository: MediaTagRepository,
    private val objectMapper: ObjectMapper,
    private val jdbcTemplate: JdbcTemplate
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

    /**
     * Tags a single media item.
     *
     * Safety/production notes:
     * - avoids wiping existing tags unless we have a valid parsed + built replacement set
     * - uses an advisory lock to prevent duplicate concurrent Claude calls for the same (mediaId, modelVersion)
     */
    @Transactional
    fun tagItem(mediaId: UUID, modelVersion: String = claudeProperties.model) {
        if (!tryAcquireAdvisoryLock(mediaId, modelVersion)) {
            log.info("Skipping tagging; advisory lock busy for mediaId={} modelVersion={}", mediaId, modelVersion)
            return
        }
        val media = mediaItemRepository.findById(mediaId).orElseThrow {
            NoSuchElementException("Media item not found: $mediaId")
        }
        val userMessage = buildString {
            appendLine("Title: \"${media.title}\"")
            appendLine("Media type: ${media.mediaType}")
        }
        val raw = claudeApiService.sendMessage(systemPrompt, userMessage)
        val parsed =
            try {
            parseClaudeTagJson(raw)
            }
            catch (e: Exception) {
                log.warn(
                    "Claude tag parse failed for mediaId={} modelVersion={}. rawPreview='{}' err={}",
                    mediaId,
                    modelVersion,
                    raw.take(300).replace("\n", "\\n"),
                    e.message
                )
                throw e
            }
        // Build all replacement MediaTagEntity objects *before* deleting anything.
        val now = Instant.now()
        val replacements = buildMediaTags(media, parsed, now, modelVersion)

        // If Claude returned effectively nothing, do NOT wipe existing tags.
        if (replacements.isEmpty()) {
            log.warn("Claude returned no usable tags; leaving existing tags intact for mediaId={} modelVersion={}", mediaId, modelVersion)
            return
        }
        // Replace tags ONLY for this (mediaId, modelVersion)
        mediaTagRepository.deleteByMediaIdAndModelVersion(mediaId, modelVersion)
        mediaTagRepository.saveAll(replacements)

        log.info("Tagged mediaId={} with {} tags (modelVersion={})", mediaId, replacements.size, modelVersion)
    }

    fun tagAllUntagged(limit: Int = 200, modelVersion: String = claudeProperties.model): List<UUID> {
        val ids = mediaTagRepository.findUntaggedMediaIds(modelVersion, limit)
        if (ids.isEmpty()) {
            return emptyList()
        }
        val succeeded = mutableListOf<UUID>()
        for (id in ids) {
            try {
                tagItem(id, modelVersion)
                succeeded += id
            }
            catch (e: Exception) {
                log.warn("Failed tagging mediaId={}: {}", id, e.message)
            }
        }
        return succeeded
    }

    private fun buildMediaTags(
        media: com.github.charlieboggus.turborecs.db.entity.MediaItemEntity,
        parsed: Map<TagCategory, Map<String, Double>>,
        now: Instant,
        modelVersion: String
    ): List<MediaTagEntity> {
        // Dedupe within this response: (category, normalizedName) -> max(weight)
        val deduped: MutableMap<Pair<TagCategory, String>, Double> = linkedMapOf()

        for ((category, tagsToWeight) in parsed) {
            for ((rawName, rawWeight) in tagsToWeight) {
                val name = normalizeTagName(rawName)
                if (name.isEmpty()) {
                    continue
                }
                val weight = sanitizeWeight(rawWeight)
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
        // Resolve TagEntity for each unique (category,name)
        val tagsByKey: MutableMap<Pair<TagCategory, String>, TagEntity> = linkedMapOf()
        for ((key, _) in deduped) {
            val (category, name) = key
            tagsByKey[key] = findOrCreateTag(category, name)
        }
        return deduped.entries.map { (key, weight) ->
            MediaTagEntity(
                id = null,
                media = media,
                tag = tagsByKey.getValue(key),
                weight = weight,
                generatedAt = now,
                modelVersion = modelVersion
            )
        }
    }

    private fun findOrCreateTag(category: TagCategory, normalizedName: String): TagEntity {
        tagRepository.findByCategoryAndNameIgnoreCase(category, normalizedName)?.let { return it }
        return try {
            tagRepository.save(
                TagEntity(
                    id = null,
                    name = normalizedName,
                    category = category
                )
            )
        }
        catch (e: DataIntegrityViolationException) {
            // Another request likely created it concurrently; re-fetch.
            tagRepository.findByCategoryAndNameIgnoreCase(category, normalizedName) ?: throw e
        }
    }

    /**
     * Parses Claude JSON into {TagCategory -> {tagName -> weight}}.
     *
     * Hard requirements:
     * - root object
     * - must include THEME, MOOD, TONE, SETTING keys
     * - each must be an object of { string -> number|string }
     */
    private fun parseClaudeTagJson(raw: String): Map<TagCategory, Map<String, Double>> {
        val cleaned = stripCodeFences(raw)

        val root =
            try {
                objectMapper.readTree(cleaned)
            }
            catch (e: Exception) {
                throw IllegalArgumentException("Claude returned invalid JSON", e)
            }

        if (!root.isObject) {
            throw IllegalArgumentException("Claude response must be a JSON object")
        }

        fun parseCategory(key: String): Map<String, Double> {
            val node = root.get(key) ?: throw IllegalArgumentException("Claude response missing required key '$key'")
            if (!node.isObject) {
                throw IllegalArgumentException("Claude response key '$key' must map to {tagName: weight}")
            }
            val out = LinkedHashMap<String, Double>()
            val it = node.fields()
            while (it.hasNext()) {
                val entry = it.next()
                val name = entry.key
                val weightNode: JsonNode = entry.value

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

        return linkedMapOf(
            TagCategory.THEME to parseCategory("THEME"),
            TagCategory.MOOD to parseCategory("MOOD"),
            TagCategory.TONE to parseCategory("TONE"),
            TagCategory.SETTING to parseCategory("SETTING"),
        )
    }

    private fun stripCodeFences(raw: String): String {
        val s = raw.trim()
        // Handle ```json ... ``` or ``` ... ```
        val fenceStart = s.startsWith("```")
        if (!fenceStart) {
            return s
        }
        // remove first line fence
        val withoutFirstFence = s
            .removePrefix("```json")
            .removePrefix("```")
            .trimStart()
        // remove trailing fence
        return withoutFirstFence
            .removeSuffix("```")
            .trim()
    }

    private fun normalizeTagName(name: String): String =
        name.trim()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("""^[\p{Punct}\s]+|[\p{Punct}\s]+$"""), "")
            .lowercase()

    private fun sanitizeWeight(raw: Double): Double {
        if (!raw.isFinite()) return 0.0
        return clamp01(raw)
    }

    private fun clamp01(x: Double): Double = min(1.0, max(0.0, x))

    /**
     * Prevent duplicate concurrent work per (mediaId, modelVersion).
     * Uses Postgres advisory locks (transaction-scoped).
     */
    private fun tryAcquireAdvisoryLock(mediaId: UUID, modelVersion: String): Boolean {
        val key = stableLockKey(mediaId, modelVersion)
        // pg_try_advisory_xact_lock(bigint) => boolean
        return jdbcTemplate.queryForObject(
            "select pg_try_advisory_xact_lock(?)",
            Boolean::class.java,
            key
        ) == true
    }

    private fun stableLockKey(mediaId: UUID, modelVersion: String): Long {
        // Deterministic 64-bit key derived from UUID + modelVersion.
        // This doesn't need to be cryptographically strong; just stable and well-distributed.
        val base = mediaId.mostSignificantBits xor mediaId.leastSignificantBits
        return base xor modelVersion.hashCode().toLong()
    }
}