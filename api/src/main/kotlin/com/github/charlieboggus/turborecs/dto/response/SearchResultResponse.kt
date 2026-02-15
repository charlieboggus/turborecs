package com.github.charlieboggus.turborecs.dto.response

import com.github.charlieboggus.turborecs.common.enums.MediaType

data class SearchResultResponse(
    val mediaType: MediaType,
    val title: String,
    val year: Int?,
    val creator: String?,
    val posterUrl: String?,
    val description: String?,
    val tmdbId: String? = null,
    val openLibraryId: String? = null
)