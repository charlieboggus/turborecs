package com.github.charlieboggus.turborecs.client.dto.claude

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ClaudeContentBlock(
    val type: String? = null,
    val text: String? = null
)