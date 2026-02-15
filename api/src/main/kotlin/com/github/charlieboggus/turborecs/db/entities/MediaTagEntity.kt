package com.github.charlieboggus.turborecs.db.entities

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "media_tags")
class MediaTagEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_id", nullable = false)
    val mediaItem: MediaItemEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    val tag: TagEntity,

    @Column(name = "weight", nullable = false)
    var weight: Double,

    @Column(name = "model_version", nullable = false)
    val modelVersion: String,

    @Column(name = "generated_at", nullable = false, updatable = false)
    val generatedAt: Instant = Instant.now()
)