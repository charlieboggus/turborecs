package com.github.charlieboggus.turborecs.web.dto

import com.github.charlieboggus.turborecs.db.entity.enums.MediaStatus
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class WatchHistoryDto(
    val id: UUID,
    val watchedAt: LocalDate,
    val rating: Int?,
    val status: MediaStatus,
    val notes: String?,
    val createdAt: Instant
)