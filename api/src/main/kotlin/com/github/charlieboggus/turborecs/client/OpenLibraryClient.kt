package com.github.charlieboggus.turborecs.client

import com.github.charlieboggus.turborecs.client.dto.openlibrary.*
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class OpenLibraryClient(private val openLibraryRestClient: RestClient) {

    /**
     * Search for books by query string.
     * Returns work-level results with basic metadata.
     */
    fun searchBooks(query: String): OpenLibrarySearchResponse {
        return openLibraryRestClient.get()
            .uri("/search.json?q={query}&limit=20&fields=key,title,author_name,first_publish_year,cover_i,number_of_pages_median,isbn,publisher", query)
            .retrieve()
            .body(OpenLibrarySearchResponse::class.java)
            ?: OpenLibrarySearchResponse()
    }

    /**
     * Fetch a work by its Open Library ID (e.g. "OL45804W").
     * Returns description, subjects, and cover IDs.
     */
    fun getWork(openLibraryId: String): OpenLibraryWork {
        return openLibraryRestClient.get()
            .uri("/works/{id}.json", openLibraryId)
            .retrieve()
            .body(OpenLibraryWork::class.java)
            ?: throw RuntimeException("Open Library work not found: $openLibraryId")
    }

    /**
     * Fetch editions for a work. Paginated — we only grab the first 50
     * since we just need to find the best English edition.
     */
    fun getEditions(openLibraryId: String): OpenLibraryEditionsResponse {
        return openLibraryRestClient.get()
            .uri("/works/{id}/editions.json?limit=50", openLibraryId)
            .retrieve()
            .body(OpenLibraryEditionsResponse::class.java)
            ?: OpenLibraryEditionsResponse()
    }
}