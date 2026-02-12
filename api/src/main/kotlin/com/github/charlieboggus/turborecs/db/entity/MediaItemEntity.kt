package com.github.charlieboggus.turborecs.db.entity

import com.github.charlieboggus.turborecs.db.entity.enums.MediaType
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "media_items")
class MediaItemEntity(

    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "uuid")
    var id: UUID? = null,

    @Column(name = "tmdb_id")
    var tmdbId: String? = null,

    @Column(name = "open_library_id")
    var openLibraryId: String? = null,

    @Column(name = "title", nullable = false)
    var title: String = "",

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    var mediaType: MediaType = MediaType.MOVIE,

    @Column(name = "year")
    var year: Int? = null,

    @Column(name = "creator")
    var creator: String? = null,

    @Column(name = "description")
    var description: String? = null,

    @Column(name = "poster_url")
    var posterUrl: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)