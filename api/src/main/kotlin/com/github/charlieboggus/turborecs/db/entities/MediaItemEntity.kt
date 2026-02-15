package com.github.charlieboggus.turborecs.db.entities

import com.github.charlieboggus.turborecs.common.enums.MediaType
import com.github.charlieboggus.turborecs.common.enums.TaggingStatus

import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "media_items")
class MediaItemEntity(

    @Id
    val id: UUID = UUID.randomUUID(),

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    val mediaType: MediaType,

    @Column(name = "title", nullable = false)
    var title: String,

    @Column(name = "year")
    var year: Int? = null,

    @Column(name = "creator")
    var creator: String? = null,

    @Column(name = "description")
    var description: String? = null,

    @Column(name = "poster_url")
    var posterUrl: String? = null,

    @Column(name = "tmdb_id")
    var tmdbId: String? = null,

    @Column(name = "open_library_id")
    var openLibraryId: String? = null,

    @Column(name = "rating")
    var rating: Int? = null,

    @Column(name = "consumed_at")
    var consumedAt: LocalDate? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "tagging_status", nullable = false)
    var taggingStatus: TaggingStatus = TaggingStatus.PENDING,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @OneToOne(mappedBy = "mediaItem", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var metadata: MediaMetadataEntity? = null,

    @OneToMany(mappedBy = "mediaItem", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val tags: MutableList<MediaTagEntity> = mutableListOf()
) {
    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }
}