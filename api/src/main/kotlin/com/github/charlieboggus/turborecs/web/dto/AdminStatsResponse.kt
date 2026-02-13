package com.github.charlieboggus.turborecs.web.dto

import com.github.charlieboggus.turborecs.db.entity.enums.MediaStatus
import com.github.charlieboggus.turborecs.db.entity.enums.MediaType

data class AdminStatsResponse(
    val totalItems: Long,
    val itemsByType: Map<MediaType, Long>,
    val latestStatusCounts: Map<MediaStatus, Long>,
    val taggedItemsForModelVersion: Long,
    val movieMetadataCount: Long,
    val bookMetadataCount: Long
)