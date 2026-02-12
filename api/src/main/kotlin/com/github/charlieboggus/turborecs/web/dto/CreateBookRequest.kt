package com.github.charlieboggus.turborecs.web.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateBookRequest(
    @field:NotBlank(message = "openLibraryId cannot be blank")
    val openLibraryId: String,

    @field:NotBlank(message = "title cannot be blank")
    @field:Size(min = 1, max = 500, message = "title must have between 1 and 500 characters")
    val title: String,

    val year: Int? = null,

    @field:Size(max = 500, message = "creator must be <= 500 characters")
    val creator: String? = null,

    val description: String? = null,

    @field:Size(max = 2000, message = "posterUrl must be <= 2000 characters")
    val posterUrl: String? = null
)