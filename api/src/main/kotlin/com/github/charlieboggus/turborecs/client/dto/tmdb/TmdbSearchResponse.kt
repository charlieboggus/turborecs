package com.github.charlieboggus.turborecs.client.dto.tmdb

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class TmdbSearchResponse(
    val results: List<TmdbSearchResult>
)