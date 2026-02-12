package com.github.charlieboggus.turborecs.web.dto

import com.github.charlieboggus.turborecs.db.entity.enums.MediaStatus
import com.github.charlieboggus.turborecs.db.entity.enums.MediaType
import java.time.Instant
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