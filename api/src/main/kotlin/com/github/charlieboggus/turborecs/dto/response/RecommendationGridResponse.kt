package com.github.charlieboggus.turborecs.dto.response

import java.util.UUID

data class RecommendationGridResponse(
    val batchId: UUID,
    val items: List<RecommendationResponse>
)