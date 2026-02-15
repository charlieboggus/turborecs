package com.github.charlieboggus.turborecs.service

import com.github.charlieboggus.turborecs.client.OpenLibraryClient
import com.github.charlieboggus.turborecs.client.TmdbClient
import com.github.charlieboggus.turborecs.common.enums.MediaType
import com.github.charlieboggus.turborecs.dto.response.SearchResultResponse
import org.springframework.stereotype.Service

@Service
class SearchService(
    private val tmdbClient: TmdbClient,
    private val openLibraryClient: OpenLibraryClient
) {

    fun search(query: String, type: MediaType): List<SearchResultResponse> {
        return when (type) {
            MediaType.MOVIE -> searchMovies(query)
            MediaType.BOOK -> searchBooks(query)
        }
    }

    private fun searchMovies(query: String): List<SearchResultResponse> {
        return tmdbClient.searchMovies(query).results.map { result ->
            SearchResultResponse(
                mediaType = MediaType.MOVIE,
                title = result.title,
                year = result.year,
                creator = null,   // TMDb search doesn't return director
                posterUrl = result.posterUrl,
                description = result.overview,
                tmdbId = result.id.toString(),
                openLibraryId = null
            )
        }
    }

    private fun searchBooks(query: String): List<SearchResultResponse> {
        return openLibraryClient.searchBooks(query).docs.map { result ->
            SearchResultResponse(
                mediaType = MediaType.BOOK,
                title = result.title,
                year = result.firstPublishYear,
                creator = result.author,
                posterUrl = result.posterUrl,
                description = null,  // search results don't include descriptions
                tmdbId = null,
                openLibraryId = result.openLibraryId
            )
        }
    }
}