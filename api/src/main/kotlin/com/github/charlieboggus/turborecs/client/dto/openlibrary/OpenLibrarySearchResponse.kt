package com.github.charlieboggus.turborecs.client.dto.openlibrary

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenLibrarySearchResponse(
    val docs: List<OpenLibrarySearchResult> = emptyList()
)