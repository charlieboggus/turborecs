package com.github.charlieboggus.turborecs.web.dto

import jakarta.validation.constraints.Size

data class UpdateNotesRequest(
    @field:Size(max = 10_000, message = "notes must be <= 10000 characters")
    val notes: String?
)