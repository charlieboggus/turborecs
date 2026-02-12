package com.github.charlieboggus.turborecs.service

import com.github.charlieboggus.turborecs.config.properties.ClaudeProperties
import com.github.charlieboggus.turborecs.db.entity.enums.TagCategory
import com.github.charlieboggus.turborecs.db.repository.MediaTagRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.math.max

data class TasteProfile(
    val themes: Map<String, Double>,
    val moods: Map<String, Double>,
    val tones: Map<String, Double>,
    val settings: Map<String, Double>,
    val topRatedTitles: List<String>,
    val lowRatedTitles: List<String>
)

@Service
class TasteProfileService(
    private val mediaTagRepository: MediaTagRepository,
    private val claudeProperties: ClaudeProperties
) {
    private val log = LoggerFactory.getLogger(TasteProfileService::class.java)

    private companion object {
        const val TOP_TAGS_PER_CATEGORY = 15
        const val TOP_TITLES = 10
    }

    /**
     * Compute taste profile on-demand from:
     * - latest rated watch_history per media item
     * - tags for the given modelVersion
     *
     * No DB persistence; this is a computed view.
     */
    fun buildTasteProfile(modelVersion: String = claudeProperties.model): TasteProfile {
        val rows = mediaTagRepository.fetchTasteRows(modelVersion)
        if (rows.isEmpty()) {
            return TasteProfile(emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyList(), emptyList())
        }

        // title -> rating (dedup per title; if multiple rows repeat, last write wins but rating should be same)
        val ratingByTitle = LinkedHashMap<String, Int>()

        // category -> (tag -> score)
        val accum: MutableMap<TagCategory, MutableMap<String, Double>> = linkedMapOf(
            TagCategory.THEME to linkedMapOf(),
            TagCategory.MOOD to linkedMapOf(),
            TagCategory.TONE to linkedMapOf(),
            TagCategory.SETTING to linkedMapOf(),
        )

        for (r in rows) {
            val title = r.getTitle().trim()
            if (title.isEmpty()) continue

            val rating = r.getRating().coerceIn(0, 5)
            ratingByTitle[title] = rating

            val category = parseCategoryOrNull(r.getCategory()) ?: run {
                log.warn("Skipping taste row with unknown category='{}' (title='{}')", r.getCategory(), title)
                continue
            }

            val tagName = normalizeTagName(r.getTagName())
            if (tagName.isEmpty()) continue

            val tagWeight = r.getTagWeight()
            val contribution = tagWeight * ratingMultiplier(rating)
            if (contribution == 0.0) continue

            val map = accum.getValue(category)
            map[tagName] = (map[tagName] ?: 0.0) + contribution
        }

        val themes = normalize(accum.getValue(TagCategory.THEME), TOP_TAGS_PER_CATEGORY)
        val moods = normalize(accum.getValue(TagCategory.MOOD), TOP_TAGS_PER_CATEGORY)
        val tones = normalize(accum.getValue(TagCategory.TONE), TOP_TAGS_PER_CATEGORY)
        val settings = normalize(accum.getValue(TagCategory.SETTING), TOP_TAGS_PER_CATEGORY)

        val topRatedTitles = ratingByTitle.entries
            .sortedByDescending { it.value }
            .take(TOP_TITLES)
            .map { it.key }

        val lowRatedTitles = ratingByTitle.entries
            .sortedBy { it.value }
            .take(TOP_TITLES)
            .map { it.key }

        return TasteProfile(
            themes = themes,
            moods = moods,
            tones = tones,
            settings = settings,
            topRatedTitles = topRatedTitles,
            lowRatedTitles = lowRatedTitles
        )
    }

    private fun parseCategoryOrNull(raw: String?): TagCategory? {
        val s = raw?.trim().orEmpty()
        if (s.isEmpty()) return null
        return try {
            TagCategory.valueOf(s)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun normalizeTagName(name: String?): String =
        name.orEmpty()
            .trim()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("""^[\p{Punct}\s]+|[\p{Punct}\s]+$"""), "")
            .lowercase()

    private fun normalize(m: Map<String, Double>, topN: Int): Map<String, Double> {
        if (m.isEmpty()) return emptyMap()
        val maxVal = m.values.fold(0.0) { acc, v -> max(acc, v) }
        if (maxVal <= 0.0) return emptyMap()

        return m.entries
            .sortedByDescending { it.value }
            .take(topN)
            .associate { it.key to (it.value / maxVal) }
    }

    private fun ratingMultiplier(rating: Int): Double = when (rating) {
        5 -> 2.0
        4 -> 1.5
        3 -> 1.0
        2 -> 0.5
        1 -> 0.0
        else -> 0.0
    }
}