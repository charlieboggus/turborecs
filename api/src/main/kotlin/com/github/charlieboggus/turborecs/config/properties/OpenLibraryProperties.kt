package com.github.charlieboggus.turborecs.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("turborecs.open-library")
data class OpenLibraryProperties(
    val baseUrl: String,
    val connectionTimeoutMs: Int = 5_000,
    val readTimeoutMs: Int = 30_000,
)