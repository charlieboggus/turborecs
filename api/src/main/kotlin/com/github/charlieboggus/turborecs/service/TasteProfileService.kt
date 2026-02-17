package com.github.charlieboggus.turborecs.service

import com.github.charlieboggus.turborecs.common.DimensionVector
import com.github.charlieboggus.turborecs.common.enums.Dimension
import com.github.charlieboggus.turborecs.common.enums.TagCategory
import com.github.charlieboggus.turborecs.config.properties.ClaudeProperties
import com.github.charlieboggus.turborecs.db.repository.MediaItemRepository
import com.github.charlieboggus.turborecs.db.repository.MediaTagRepository
import com.github.charlieboggus.turborecs.db.repository.MediaVectorRepository
import com.github.charlieboggus.turborecs.dto.response.TasteProfileResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.max

@Service
class TasteProfileService(
    private val claudeProperties: ClaudeProperties,
    private val mediaTagRepository: MediaTagRepository,
    private val mediaItemRepository: MediaItemRepository,
    private val mediaVectorRepository: MediaVectorRepository
) {

    // ── Public API ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    fun buildTasteProfile(): TasteProfileResponse {
        val modelVersion = claudeProperties.model

        // Legacy tag aggregation (unchanged)
        val tagResult = buildTagProfile(modelVersion)

        // Vector aggregation (new)
        val vectorResult = buildVectorProfile(modelVersion)

        return TasteProfileResponse(
            themes = tagResult.themes,
            moods = tagResult.moods,
            tones = tagResult.tones,
            settings = tagResult.settings,
            topRatedTitles = tagResult.topRatedTitles,
            lowRatedTitles = tagResult.lowRatedTitles,
            tasteVector = vectorResult.tasteVector.toSortedMap(),
            antiVector = vectorResult.antiVector.toSortedMap(),
            vectorCoverage = vectorResult.coverage
        )
    }

    // ── Vector Profile ──────────────────────────────────────────────────────

    private data class VectorProfileResult(
        val tasteVector: DimensionVector,
        val antiVector: DimensionVector,
        val coverage: Double
    )

    private fun buildVectorProfile(modelVersion: String): VectorProfileResult {
        val ratedItems = mediaItemRepository.findAll().filter { it.rating != null }
        if (ratedItems.isEmpty()) {
            return VectorProfileResult(DimensionVector.zero(), DimensionVector.zero(), 0.0)
        }

        val ratedIds = ratedItems.map { it.id }.toSet()
        val vectors = mediaVectorRepository.findAllByModelVersionAndMediaItemIds(
            modelVersion, ratedIds
        ).associateBy { it.mediaItemId }

        val itemsWithVectors = ratedItems.filter { it.id in vectors }
        val coverage = if (ratedItems.isNotEmpty()) {
            itemsWithVectors.size.toDouble() / ratedItems.size
        } else 0.0

        if (itemsWithVectors.isEmpty()) {
            return VectorProfileResult(DimensionVector.zero(), DimensionVector.zero(), 0.0)
        }

        // Positive aggregation: items rated 3-5
        val posAccum = mutableMapOf<Dimension, Double>()
        var posWeight = 0.0

        // Negative aggregation: items rated 1-2
        val negAccum = mutableMapOf<Dimension, Double>()
        var negWeight = 0.0

        for (item in itemsWithVectors) {
            val vec = vectors[item.id]!!.toDimensionVector()
            val rating = item.rating!!
            val mult = ratingMultiplier(rating)

            if (rating >= 3) {
                for (d in Dimension.entries) {
                    posAccum[d] = (posAccum[d] ?: 0.0) + vec[d] * mult
                }
                posWeight += mult
            } else {
                for (d in Dimension.entries) {
                    negAccum[d] = (negAccum[d] ?: 0.0) + vec[d] * mult
                }
                negWeight += mult
            }
        }

        // Weighted average, then clamp to 0..1
        val tasteScores = mutableMapOf<Dimension, Double>()
        val antiScores = mutableMapOf<Dimension, Double>()

        for (d in Dimension.entries) {
            tasteScores[d] = if (posWeight > 0) ((posAccum[d] ?: 0.0) / posWeight).coerceIn(0.0, 1.0) else 0.0
            antiScores[d] = if (negWeight > 0) ((negAccum[d] ?: 0.0) / negWeight).coerceIn(0.0, 1.0) else 0.0
        }

        return VectorProfileResult(
            tasteVector = DimensionVector(tasteScores),
            antiVector = DimensionVector(antiScores),
            coverage = coverage
        )
    }

    private fun ratingMultiplier(rating: Int): Double = when (rating) {
        5 -> 2.0
        4 -> 1.5
        3 -> 1.0
        2 -> 1.5 // strong negative signal
        1 -> 2.0 // strongest negative signal
        else -> 0.0
    }
    
    private data class TagProfileResult(
        val themes: Map<String, Double>,
        val moods: Map<String, Double>,
        val tones: Map<String, Double>,
        val settings: Map<String, Double>,
        val topRatedTitles: List<String>,
        val lowRatedTitles: List<String>
    )

    private fun buildTagProfile(modelVersion: String): TagProfileResult {
        val items = mediaTagRepository.findAllByModelVersion(modelVersion)
        if (items.isEmpty()) {
            return TagProfileResult(emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyList(), emptyList())
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
            if (title.isEmpty()) continue

            val rating = item.mediaItem.rating ?: continue
            ratingByTitle[title] = rating

            val category = parseCategory(item.tag.category.toString()) ?: continue
            val tagName = normalizeTagName(item.tag.name)
            if (tagName.isEmpty()) continue

            val contribution = item.weight * getTagRatingMultiplier(rating)
            if (contribution <= 0.0) continue

            val map = acc.getValue(category)
            map[tagName] = (map[tagName] ?: 0.0) + contribution
        }

        val topRatedTitles = ratingByTitle.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }
        val lowRatedTitles = ratingByTitle.entries
            .sortedBy { it.value }
            .take(5)
            .map { it.key }

        return TagProfileResult(
            themes = normalize(acc.getValue(TagCategory.THEME), 15),
            moods = normalize(acc.getValue(TagCategory.MOOD), 15),
            tones = normalize(acc.getValue(TagCategory.TONE), 15),
            settings = normalize(acc.getValue(TagCategory.SETTING), 15),
            topRatedTitles = topRatedTitles,
            lowRatedTitles = lowRatedTitles
        )
    }

    private fun parseCategory(raw: String?): TagCategory? {
        val s = raw?.trim().orEmpty()
        if (s.isEmpty()) return null
        return try { TagCategory.valueOf(s) } catch (_: IllegalArgumentException) { null }
    }

    private fun normalizeTagName(name: String?): String =
        name.orEmpty().trim()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("""^[\p{Punct}\s]+|[\p{Punct}\s]+$"""), "")
            .lowercase()

    private fun normalize(m: Map<String, Double>, topN: Int): Map<String, Double> {
        if (m.isEmpty()) return emptyMap()
        val maxVal = m.values.fold(0.0) { a, b -> max(a, b) }
        if (maxVal <= 0.0) return emptyMap()
        return m.entries
            .sortedByDescending { it.value }
            .take(topN)
            .associate { it.key to (it.value / maxVal) }
    }

    private fun getTagRatingMultiplier(rating: Int): Double = when (rating) {
        5 -> 2.0; 4 -> 1.5; 3 -> 1.0; 2 -> 0.5; 1 -> 0.0; else -> 0.0
    }
}