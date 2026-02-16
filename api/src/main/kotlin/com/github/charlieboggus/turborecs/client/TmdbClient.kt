package com.github.charlieboggus.turborecs.client

import com.github.charlieboggus.turborecs.client.dto.tmdb.TmdbMovieDetail
import com.github.charlieboggus.turborecs.client.dto.tmdb.TmdbSearchResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import kotlin.system.measureTimeMillis

@Component
class TmdbClient(private val tmdbRestClient: RestClient) {

    private val log = LoggerFactory.getLogger(this::class.java)

    fun searchMovies(query: String): TmdbSearchResponse {
        val responseEntity: TmdbSearchResponse
        val responseMs = measureTimeMillis {
            responseEntity = tmdbRestClient.get()
                .uri("/search/movie?query={query}", query)
                .retrieve()
                .body(TmdbSearchResponse::class.java)
                ?: TmdbSearchResponse(emptyList())
        }
        log.debug("TMDB search response returned in $responseMs ms")
        return responseEntity
    }

    fun getMovie(tmdbId: String): TmdbMovieDetail {
        val responseEntity: TmdbMovieDetail
        val responseMs = measureTimeMillis {
            responseEntity = tmdbRestClient.get()
                .uri("/movie/{id}?append_to_response=credits", tmdbId)
                .retrieve()
                .body(TmdbMovieDetail::class.java)
                ?: throw RuntimeException("TMDb movie not found: $tmdbId")
        }
        log.debug("TMDB get movie response returned in $responseMs ms")
        return responseEntity
    }
}