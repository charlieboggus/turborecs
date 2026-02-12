package com.github.charlieboggus.turborecs.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "turborecs.claude")
data class ClaudeProperties(
    val apiKey: String,
    val model: String,
    val maxTokens: Int = 1024,
    val connectTimeoutMs: Int = 5_000,
    val readTimeoutMs: Int = 30_000
)