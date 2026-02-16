package com.github.charlieboggus.turborecs.dto.request

import com.github.charlieboggus.turborecs.common.enums.MediaType

data class ExcludeMediaRequest(
    val mediaType: MediaType,
    val title: String,
    val year: Int? = null,
    val tmdbId: String? = null,
    val openLibraryId: String? = null,
    val reason: String? = null
)