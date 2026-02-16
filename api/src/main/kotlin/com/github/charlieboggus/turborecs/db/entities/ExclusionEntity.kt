package com.github.charlieboggus.turborecs.db.entities

import com.github.charlieboggus.turborecs.common.enums.MediaType

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "exclusions")
class ExclusionEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "title", nullable = false)
    val title: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    val mediaType: MediaType,

    @Column(name = "year")
    val year: Int? = null,

    @Column(name = "tmdb_id")
    val tmdbId: String? = null,

    @Column(name = "open_library_id")
    val openLibraryId: String? = null,

    @Column(name = "reason")
    var reason: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)