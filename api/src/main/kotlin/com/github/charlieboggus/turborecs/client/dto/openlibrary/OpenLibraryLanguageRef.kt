package com.github.charlieboggus.turborecs.client.dto.openlibrary

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenLibraryLanguageRef(
    val key: String                                         // "/languages/eng"
)