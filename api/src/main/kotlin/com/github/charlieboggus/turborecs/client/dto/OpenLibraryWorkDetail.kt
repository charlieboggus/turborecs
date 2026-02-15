package com.github.charlieboggus.turborecs.client.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenLibraryWorkDetail(
    val key: String,
    val title: String,
    val description: Any?,                        // can be String or { "value": "..." }
    val subjects: List<String>?,
    val covers: List<Int>?
) {
    val openLibraryId: String get() = key.removePrefix("/works/")

    val posterUrl: String? get() = covers?.firstOrNull()?.let { "https://covers.openlibrary.org/b/id/$it-L.jpg" }

    val descriptionText: String? get() = when (description) {
        is String -> description
        is Map<*, *> -> (description as Map<*, *>)["value"] as? String
        else -> null
    }
}