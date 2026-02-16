package com.github.charlieboggus.turborecs.client.dto.openlibrary

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenLibrarySearchResult(
    val key: String,                                        // "/works/OL45804W"
    val title: String,
    @JsonProperty("author_name") val authorName: List<String>?,
    @JsonProperty("first_publish_year") val firstPublishYear: Int?,
    @JsonProperty("cover_i") val coverId: Int?,
    @JsonProperty("number_of_pages_median") val pageCountMedian: Int?,
    val isbn: List<String>?,
    val publisher: List<String>?
) {
    val openLibraryId: String get() = key.removePrefix("/works/")
    val author: String? get() = authorName?.firstOrNull()
    val posterUrl: String? get() = coverId?.let { "https://covers.openlibrary.org/b/id/$it-L.jpg" }
}