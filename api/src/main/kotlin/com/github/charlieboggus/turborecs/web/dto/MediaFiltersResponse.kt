package com.github.charlieboggus.turborecs.web.dto

import com.github.charlieboggus.turborecs.db.entity.enums.MediaStatus
import com.github.charlieboggus.turborecs.db.entity.enums.MediaType

data class MediaFiltersResponse(
    val types: List<MediaType>,
    val statuses: List<MediaStatus>,
    val genres: List<String>,
    val topTags: List<TagSummaryDto>
)

data class TagSummaryDto(
    val category: String,
    val name: String,
    val count: Long
)
