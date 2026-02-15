package com.github.charlieboggus.turborecs.client.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class TmdbMovieDetail(
    val id: Int,
    val title: String,
    @JsonProperty("release_date") val releaseDate: String?,
    @JsonProperty("poster_path") val posterPath: String?,
    val overview: String?,
    val runtime: Int?,
    val genres: List<TmdbGenre>
) {
    val year: Int? get() = releaseDate?.take(4)?.toIntOrNull()
    val posterUrl: String? get() = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
    val director: String? get() = null // populated from /credits endpoint if needed
}