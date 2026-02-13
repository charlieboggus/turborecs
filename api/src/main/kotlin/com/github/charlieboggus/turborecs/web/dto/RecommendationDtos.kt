package com.github.charlieboggus.turborecs.web.dto

import com.github.charlieboggus.turborecs.db.entity.enums.MediaType
import com.github.charlieboggus.turborecs.db.entity.enums.RecommendationSelection
import java.util.UUID

data class RecommendationTileResponse(
    val slot: Int,
    val id: UUID,
    val title: String,
    val type: MediaType,
    val year: Int?,
    val creator: String?,
    val reason: String,
    val matchedThemes: List<String>
)

data class RecommendationGridResponse(
    val batchId: UUID,
    val selection: RecommendationSelection,
    val items: List<RecommendationTileResponse>
)