package com.github.charlieboggus.turborecs.config

import com.github.charlieboggus.turborecs.config.properties.ClaudeProperties
import com.github.charlieboggus.turborecs.config.properties.OpenLibraryProperties
import com.github.charlieboggus.turborecs.config.properties.TmdbProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient

@Configuration
class RestClientConfig(
    private val claudeProperties: ClaudeProperties,
    private val tmdbProperties: TmdbProperties,
    private val openLibraryProperties: OpenLibraryProperties
) {
    @Bean
    fun tmdbRestClient(): RestClient = RestClient.builder()
        .baseUrl(tmdbProperties.baseUrl)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader("Authorization", "Bearer ${tmdbProperties.apiKey}")
        .build()

    @Bean
    fun openLibraryRestClient(): RestClient = RestClient.builder()
        .baseUrl(openLibraryProperties.baseUrl)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader("User-Agent", openLibraryProperties.userAgent)
        .build()

    @Bean
    fun claudeRestClient(): RestClient = RestClient.builder()
        .baseUrl(claudeProperties.baseUrl)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader("x-api-key", claudeProperties.apiKey)
        .defaultHeader("anthropic-version", "2023-06-01")
        .build()
}