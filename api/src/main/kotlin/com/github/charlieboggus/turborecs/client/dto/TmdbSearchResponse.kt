package com.github.charlieboggus.turborecs.client.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class TmdbSearchResponse(
    val results: List<TmdbSearchResult>
)