package com.github.charlieboggus.turborecs.web.dto

data class UpdateMediaItemRequest(
    val title: String? = null,
    val year: Int? = null,
    val creator: String? = null,
    val description: String? = null,
    val posterUrl: String? = null
)
