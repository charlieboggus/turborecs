package com.github.charlieboggus.turborecs.web.dto

import com.github.charlieboggus.turborecs.db.entity.enums.MediaStatus
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import java.util.UUID

data class BulkStatusRequest(
    @field:Size(min = 1, max = 500)
    val ids: List<UUID>,
    val status: MediaStatus
)

data class BulkRatingRequest(
    @field:Size(min = 1, max = 500)
    val ids: List<UUID>,
    @field:Min(1) @field:Max(5)
    val rating: Int
)

data class BulkNotesRequest(
    @field:Size(min = 1, max = 500)
    val ids: List<UUID>,
    val notes: String?
)