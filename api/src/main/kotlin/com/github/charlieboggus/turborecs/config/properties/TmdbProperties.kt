package com.github.charlieboggus.turborecs.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("turborecs.tmdb")
data class TmdbProperties(
    val baseUrl: String,
    val apiKey: String,
    val connectionTimeoutMs: Int = 5_000,
    val readTimeoutMs: Int = 30_000,
)