package com.github.charlieboggus.turborecs.dto.response

import com.github.charlieboggus.turborecs.common.enums.MediaType
import com.github.charlieboggus.turborecs.db.entities.RecommendationLogEntity
import java.util.UUID

data class RecommendationResponse(
    val id: UUID,
    val mediaType: MediaType,
    val title: String,
    val year: Int?,
    val creator: String?,
    val reason: String,
    val matchedTags: List<String>
) {
    companion object {
        fun from(entity: RecommendationLogEntity) = RecommendationResponse(
            id = entity.id,
            mediaType = entity.mediaType,
            title = entity.title,
            year = entity.year,
            creator = entity.creator,
            reason = entity.reason,
            matchedTags = entity.matchedTags
        )
    }
}