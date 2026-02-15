package com.github.charlieboggus.turborecs.client

import com.github.charlieboggus.turborecs.client.dto.OpenLibrarySearchResponse
import com.github.charlieboggus.turborecs.client.dto.OpenLibraryWorkDetail
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class OpenLibraryClient(private val openLibraryRestClient: RestClient) {

    fun searchBooks(query: String): OpenLibrarySearchResponse {
        return openLibraryRestClient.get()
            .uri("/search.json?q={query}&limit=20", query)
            .retrieve()
            .body(OpenLibrarySearchResponse::class.java)
            ?: OpenLibrarySearchResponse(docs = emptyList())
    }

    fun getWork(openLibraryId: String): OpenLibraryWorkDetail {
        return openLibraryRestClient.get()
            .uri("/works/{id}.json", openLibraryId)
            .retrieve()
            .body(OpenLibraryWorkDetail::class.java)
            ?: throw RuntimeException("Open Library work not found: $openLibraryId")
    }
}