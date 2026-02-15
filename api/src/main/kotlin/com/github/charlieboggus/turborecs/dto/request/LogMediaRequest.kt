package com.github.charlieboggus.turborecs.dto.request

import com.github.charlieboggus.turborecs.common.enums.MediaType
import java.time.LocalDate

data class LogMediaRequest(
    val mediaType: MediaType,
    val tmdbId: String? = null,
    val openLibraryId: String? = null,
    val rating: Int? = null,
    val consumedAt: LocalDate? = null,
)