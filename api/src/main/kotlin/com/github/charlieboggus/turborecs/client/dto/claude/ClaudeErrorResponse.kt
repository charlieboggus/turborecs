package com.github.charlieboggus.turborecs.client.dto.claude

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ClaudeErrorResponse(
    val error: ClaudeError? = null
)