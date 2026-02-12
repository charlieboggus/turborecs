package com.github.charlieboggus.turborecs.web.dto

import com.github.charlieboggus.turborecs.db.entity.enums.MediaStatus
import jakarta.validation.constraints.NotNull

data class UpdateStatusRequest(
    @field:NotNull(message = "status is required")
    val status: MediaStatus
)