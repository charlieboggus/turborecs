package com.github.charlieboggus.turborecs.client.dto.tmdb

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class TmdbCrewMember(
    val name: String,
    val job: String
)