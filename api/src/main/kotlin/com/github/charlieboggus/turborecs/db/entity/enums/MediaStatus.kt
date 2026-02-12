package com.github.charlieboggus.turborecs.db.entity.enums

enum class MediaStatus {
    WATCHED,        // For movies
    FINISHED,       // For books
    READING,        // Currently reading
    WATCHING,       // Currently watching (series)
    WANT_TO_WATCH,
    WANT_TO_READ,
    DROPPED
}