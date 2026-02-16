package com.github.charlieboggus.turborecs.dto.response

import com.github.charlieboggus.turborecs.common.enums.MediaType
import com.github.charlieboggus.turborecs.db.entities.ExclusionEntity
import java.time.Instant
import java.util.UUID

data class ExclusionResponse(
    val id: UUID,
    val mediaType: MediaType,
    val title: String,
    val year: Int?,
    val reason: String?,
    val createdAt: Instant
) {
    companion object {
        fun from(entity: ExclusionEntity) = ExclusionResponse(
            id = entity.id,
            mediaType = entity.mediaType,
            title = entity.title,
            year = entity.year,
            reason = entity.reason,
            createdAt = entity.createdAt
        )
    }
}