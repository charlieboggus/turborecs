package com.github.charlieboggus.turborecs.client.dto.openlibrary

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenLibraryEdition(
    val key: String,                                        // "/books/OL44247403M"
    val title: String,
    val publishers: List<String>? = null,
    @JsonProperty("publish_date") val publishDate: String? = null,
    @JsonProperty("number_of_pages") val numberOfPages: Int? = null,
    @JsonProperty("isbn_13") val isbn13: List<String>? = null,
    @JsonProperty("isbn_10") val isbn10: List<String>? = null,
    val languages: List<OpenLibraryLanguageRef>? = null,
    val covers: List<Int>? = null
) {
    val isbn: String? get() = isbn13?.firstOrNull() ?: isbn10?.firstOrNull()
    val publisher: String? get() = publishers?.firstOrNull()
    val isEnglish: Boolean get() = languages?.any { it.key == "/languages/eng" } != false

    val posterUrl: String? get() = covers
        ?.firstOrNull { it > 0 }
        ?.let { "https://covers.openlibrary.org/b/id/$it-L.jpg" }
}