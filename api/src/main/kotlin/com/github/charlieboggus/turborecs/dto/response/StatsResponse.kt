package com.github.charlieboggus.turborecs.dto.response

import com.github.charlieboggus.turborecs.common.enums.MediaType

data class StatsResponse(
    val totalItems: Long,
    val movieCount: Long,
    val bookCount: Long,
    val uniqueTagCount: Long,
    val tagAssignmentCount: Long,
    val recommendationCount: Long,
    val exclusionCount: Long,
    val vectorCount: Long
)