package com.github.charlieboggus.turborecs.client.dto.claude

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class ClaudeUsage(
    @JsonProperty("input_tokens")
    val inputTokens: Int? = null,
    @JsonProperty("output_tokens")
    val outputTokens: Int? = null
)