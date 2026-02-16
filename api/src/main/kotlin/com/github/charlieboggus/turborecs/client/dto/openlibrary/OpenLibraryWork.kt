package com.github.charlieboggus.turborecs.client.dto.openlibrary

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenLibraryWork(
    val key: String,                                        // "/works/OL45804W"
    val title: String,
    val description: Any? = null,                           // String or { "type": "...", "value": "..." }
    val subjects: List<String>? = null,
    val covers: List<Int>? = null
) {
    val openLibraryId: String get() = key.removePrefix("/works/")

    val posterUrl: String? get() = covers
        ?.firstOrNull { it > 0 }
        ?.let { "https://covers.openlibrary.org/b/id/$it-L.jpg" }

    val descriptionText: String? get() = when (description) {
        is String -> description
        is Map<*, *> -> (description as Map<*, *>)["value"] as? String
        else -> null
    }
}