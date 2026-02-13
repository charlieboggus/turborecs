package com.github.charlieboggus.turborecs.web.dto

import com.github.charlieboggus.turborecs.db.entity.enums.MediaStatus
import com.github.charlieboggus.turborecs.db.entity.enums.MediaType
import com.github.charlieboggus.turborecs.db.entity.enums.TagCategory
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class MediaItemResponse(
    val id: UUID,
    val type: MediaType,
    val title: String,
    val year: Int?,
    val creator: String?,
    val description: String?,
    val posterUrl: String?,
    val createdAt: Instant,
    val updatedAt: Instant,

    // derived from latest watch history
    val latestStatus: MediaStatus?,
    val latestRating: Int?
)

data class MediaItemDetailResponse(
    val id: UUID,
    val type: MediaType,
    val title: String,
    val year: Int?,
    val creator: String?,
    val description: String?,
    val posterUrl: String?,
    val createdAt: Instant,
    val updatedAt: Instant,

    // Latest user state (from watch_history)
    val latestStatus: MediaStatus?,
    val latestRating: Int?,
    val latestNotes: String?,
    val latestWatchedAt: LocalDate?,

    // Type-specific metadata
    val movieMetadata: MovieMetadataDto?,
    val bookMetadata: BookMetadataDto?,

    // Optional: tags
    val tags: List<TagWeightDto>
)

data class MovieMetadataDto(
    val runtimeMinutes: Int?,
    val genres: List<String>
)

data class BookMetadataDto(
    val pageCount: Int?,
    val isbn: String?,
    val publisher: String?
)

data class TagWeightDto(
    val category: TagCategory,
    val name: String,
    val weight: Double
)