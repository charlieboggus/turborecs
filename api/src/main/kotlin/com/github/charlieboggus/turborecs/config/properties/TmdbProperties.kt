package com.github.charlieboggus.turborecs.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("turborecs.tmdb")
data class TmdbProperties(
    val baseUrl: String,
    val apiKey: String
)