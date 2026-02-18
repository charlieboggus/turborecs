package com.github.charlieboggus.turborecs.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "turborecs.claude")
data class ClaudeProperties(
    val baseUrl: String,
    val apiKey: String,
    val model: String
)