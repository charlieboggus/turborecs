package com.github.charlieboggus.turborecs.client.dto.openlibrary

data class EnrichedBookData(
    val openLibraryId: String,
    val title: String,
    val author: String?,
    val year: Int?,
    val description: String?,
    val posterUrl: String?,
    val genres: List<String>,
    val pageCount: Int?,
    val isbn: String?,
    val publisher: String?
) {
    companion object {
        /**
         * Build enriched data from a work + the best edition we could find.
         * Falls back to search result data for fields the work/edition don't have.
         */
        fun from(
            work: OpenLibraryWork,
            edition: OpenLibraryEdition?,
            searchResult: OpenLibrarySearchResult?
        ): EnrichedBookData {
            return EnrichedBookData(
                openLibraryId = work.openLibraryId,
                title = work.title,
                author = searchResult?.author,              // search is the best source for author name
                year = searchResult?.firstPublishYear,
                description = work.descriptionText,
                posterUrl = edition?.posterUrl ?: work.posterUrl ?: searchResult?.posterUrl,
                genres = work.subjects?.take(10) ?: emptyList(),
                pageCount = edition?.numberOfPages ?: searchResult?.pageCountMedian,
                isbn = edition?.isbn ?: searchResult?.isbn?.firstOrNull(),
                publisher = edition?.publisher ?: searchResult?.publisher?.firstOrNull()
            )
        }
    }
}