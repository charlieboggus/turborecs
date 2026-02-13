package com.github.charlieboggus.turborecs.web.dto

import java.util.UUID

data class TagDto(
    val id: UUID,
    val category: String,
    val name: String
)