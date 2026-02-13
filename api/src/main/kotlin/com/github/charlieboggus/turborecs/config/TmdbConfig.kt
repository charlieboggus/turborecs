package com.github.charlieboggus.turborecs.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient

@Configuration
class TmdbConfig(
    @Value("\${turborecs.tmdb.api.key}") private val tmdbApiKey: String
) {
    @Bean
    fun tmdbRestClient(): RestClient =
        RestClient.builder()
            .baseUrl("https://api.themoviedb.org/3")
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer $tmdbApiKey")
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build()
}