package com.github.charlieboggus.turborecs.client.dto.claude

import com.fasterxml.jackson.annotation.JsonProperty

data class ClaudeMessageRequest(
    val model: String,
    @JsonProperty("max_tokens")
    val maxTokens: Int,
    val system: String,
    val messages: List<ClaudeMessage>
)