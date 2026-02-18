package com.github.charlieboggus.turborecs.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("turborecs.open-library")
data class OpenLibraryProperties(
    val baseUrl: String,
    val userAgent: String
)