package com.github.charlieboggus.turborecs.client

import com.github.charlieboggus.turborecs.client.dto.TmdbMovieDetail
import com.github.charlieboggus.turborecs.client.dto.TmdbSearchResponse
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class TmdbClient(private val tmdbResClient: RestClient) {

    fun searchMovies(query: String): TmdbSearchResponse {
        return tmdbResClient.get()
            .uri("/search/movie?query={query}", query)
            .retrieve()
            .body(TmdbSearchResponse::class.java)
            ?: TmdbSearchResponse(emptyList())
    }

    fun getMovie(tmdbId: String): TmdbMovieDetail {
        return tmdbResClient.get()
            .uri("/movie/{id}", tmdbId)
            .retrieve()
            .body(TmdbMovieDetail::class.java)
            ?: throw RuntimeException("TMDb movie not found: $tmdbId")
    }
}