package com.github.charlieboggus.turborecs.common

import com.github.charlieboggus.turborecs.common.enums.Dimension

/**
 * A simple map wrapper representing a vector of dimension scores.
 * Each dimension is 0.0–1.0.
 */
data class DimensionVector(
    val scores: Map<Dimension, Double>
) {
    operator fun get(d: Dimension): Double = scores.getOrDefault(d, 0.0)

    fun toSortedMap(): LinkedHashMap<String, Double> {
        val result = LinkedHashMap<String, Double>()
        for (d in Dimension.entries) {
            result[d.name] = scores.getOrDefault(d, 0.0)
        }
        return result
    }

    fun topDimensions(n: Int): List<Pair<Dimension, Double>> =
        scores.entries
            .sortedByDescending { it.value }
            .take(n)
            .map { it.key to it.value }

    companion object {
        fun zero(): DimensionVector = DimensionVector(
            Dimension.entries.associateWith { 0.0 }
        )

        fun fromMap(map: Map<String, Double>): DimensionVector {
            val scores = mutableMapOf<Dimension, Double>()
            for (d in Dimension.entries) {
                scores[d] = (map[d.name] ?: 0.0).coerceIn(0.0, 1.0)
            }
            return DimensionVector(scores)
        }
    }
}