package com.github.charlieboggus.turborecs.dto.response

data class TasteProfileResponse(
    // Legacy tag-based maps (still populated, used by frontend)
    val themes: Map<String, Double>,
    val moods: Map<String, Double>,
    val tones: Map<String, Double>,
    val settings: Map<String, Double>,
    val topRatedTitles: List<String>,
    val lowRatedTitles: List<String>,

    // New dimension vector system
    val tasteVector: Map<String, Double> = emptyMap(),
    val antiVector: Map<String, Double> = emptyMap(),
    val vectorCoverage: Double = 0.0 // fraction of rated items that have vectors
)