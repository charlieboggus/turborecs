package com.github.charlieboggus.turborecs.service

import com.github.charlieboggus.turborecs.common.enums.TagCategory
import com.github.charlieboggus.turborecs.config.properties.ClaudeProperties
import com.github.charlieboggus.turborecs.db.repository.MediaTagRepository
import com.github.charlieboggus.turborecs.dto.response.TasteProfileResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.max

@Service
class TasteProfileService(
    private val claudeProperties: ClaudeProperties,
    private val mediaTagRepository: MediaTagRepository
) {
    @Transactional(readOnly = true)
    fun buildTasteProfile(): TasteProfileResponse {
        val modelVersion = claudeProperties.model
        val items = mediaTagRepository.findAllByModelVersion(modelVersion)
        if (items.isEmpty()) {
            return TasteProfileResponse(emptyMap(), emptyMap(), emptyMap(),
                emptyMap(), emptyList(), emptyList())
        }
        val ratingByTitle = LinkedHashMap<String, Int>()
        val acc: MutableMap<TagCategory, MutableMap<String, Double>> = linkedMapOf(
            TagCategory.THEME to linkedMapOf(),
            TagCategory.MOOD to linkedMapOf(),
            TagCategory.TONE to linkedMapOf(),
            TagCategory.SETTING to linkedMapOf()
        )
        for (item in items) {
            val title = item.mediaItem.title.trim()
            if (title.isEmpty()) {
                continue
            }

            val rating = item.mediaItem.rating!!
            ratingByTitle[title] = rating

            val category = parseCategory(item.tag.category.toString()) ?: run {
                continue
            }

            val tagName = normalizeTagName(item.tag.name)
            if (tagName.isEmpty()) {
                continue
            }

            val tagWeight = item.weight
            val contribution = tagWeight * getRatingMultiplier(rating)
            if (contribution <= 0.0) {
                continue
            }

            val map = acc.getValue(category)
            map[tagName] = (map[tagName] ?: 0.0) + contribution
        }
        val themes = normalize(acc.getValue(TagCategory.THEME), 15)
        val moods = normalize(acc.getValue(TagCategory.MOOD), 15)
        val tones = normalize(acc.getValue(TagCategory.TONE), 15)
        val settings = normalize(acc.getValue(TagCategory.SETTING), 15)
        val topRatedTitles = ratingByTitle.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }
        val lowRatedTitles = ratingByTitle.entries
            .sortedBy { it.value }
            .take(5)
            .map { it.key }

        return TasteProfileResponse(
            themes = themes,
            moods = moods,
            tones = tones,
            settings = settings,
            topRatedTitles = topRatedTitles,
            lowRatedTitles = lowRatedTitles
        )
    }

    private fun parseCategory(raw: String?): TagCategory? {
        val s = raw?.trim().orEmpty()
        if (s.isEmpty()) {
            return null
        }
        return try {
            TagCategory.valueOf(s)
        }
        catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun normalizeTagName(name: String?): String {
        return name.orEmpty()
            .trim()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("""^[\p{Punct}\s]+|[\p{Punct}\s]+$"""), "")
            .lowercase()
    }

    private fun normalize(m: Map<String, Double>, topN: Int): Map<String, Double> {
        if (m.isEmpty()) {
            return emptyMap()
        }
        val maxVal = m.values.fold(0.0) { a, b -> max(a, b) }
        if (maxVal <= 0.0) {
            return emptyMap()
        }
        return m.entries
            .sortedByDescending { it.value }
            .take(topN)
            .associate { it.key to (it.value / maxVal) }
    }

    private fun getRatingMultiplier(rating: Int): Double = when (rating) {
        5 -> 2.0
        4 -> 1.5
        3 -> 1.0
        2 -> 0.5
        1 -> 0.0
        else -> 0.0
    }
}