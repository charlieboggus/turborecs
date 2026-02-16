package com.github.charlieboggus.turborecs.dto.response

import com.github.charlieboggus.turborecs.common.enums.MediaType
import com.github.charlieboggus.turborecs.common.enums.TaggingStatus
import com.github.charlieboggus.turborecs.db.entities.MediaItemEntity
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class MediaItemResponse(
    val id: UUID,
    val mediaType: MediaType,
    val title: String,
    val year: Int?,
    val creator: String?,
    val description: String?,
    val posterUrl: String?,
    val rating: Int?,
    val consumedAt: LocalDate?,
    val taggingStatus: TaggingStatus,
    val createdAt: Instant,
    val updatedAt: Instant,

    val genres: List<String>,
    val runtimeMinutes: Int?,
    val pageCount: Int?,
    val isbn: String?,
    val publisher: String?
) {
    companion object {
        fun from(entity: MediaItemEntity): MediaItemResponse {
            val meta = entity.metadata
            return MediaItemResponse(
                id = entity.id,
                mediaType = entity.mediaType,
                title = entity.title,
                year = entity.year,
                creator = entity.creator,
                description = entity.description,
                posterUrl = entity.posterUrl,
                rating = entity.rating,
                consumedAt = entity.consumedAt,
                taggingStatus = entity.taggingStatus,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                genres = meta?.genres ?: emptyList(),
                runtimeMinutes = meta?.runtimeMinutes,
                pageCount = meta?.pageCount,
                isbn = meta?.isbn,
                publisher = meta?.publisher
            )
        }
    }
}