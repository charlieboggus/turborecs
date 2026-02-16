package com.github.charlieboggus.turborecs.dto.response

data class TasteProfileResponse(
    val themes: Map<String, Double>,
    val moods: Map<String, Double>,
    val tones: Map<String, Double>,
    val settings: Map<String, Double>,
    val topRatedTitles: List<String>,
    val lowRatedTitles: List<String>
)