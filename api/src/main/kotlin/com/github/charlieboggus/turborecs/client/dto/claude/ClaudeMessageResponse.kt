package com.github.charlieboggus.turborecs.client.dto.claude

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ClaudeMessageResponse(
    val model: String? = null,
    val content: List<ClaudeContentBlock> = emptyList(),
    val usage: ClaudeUsage? = null
)