package com.github.charlieboggus.turborecs.common.enums

enum class MediaSort {
    RATING,
    TITLE,
    DATE_ADDED;

    companion object {
        fun from(value: String?): MediaSort? = value?.let {
            runCatching { valueOf(it.uppercase()) }.getOrNull()
        }
    }
}