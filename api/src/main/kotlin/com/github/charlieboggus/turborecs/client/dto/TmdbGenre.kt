package com.github.charlieboggus.turborecs.client.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class TmdbGenre(
    val id: Int,
    val name: String
)