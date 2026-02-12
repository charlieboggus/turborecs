package com.github.charlieboggus.turborecs.db.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "media_tags",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_media_tags_media_tag_model",
            columnNames = ["media_id", "tag_id", "model_version"]
        )
    ]
)
class MediaTagEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "media_id", nullable = false)
    var media: MediaItemEntity,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id", nullable = false)
    var tag: TagEntity,

    @Column(name = "weight", nullable = false)
    var weight: Double,

    @Column(name = "generated_at", nullable = false)
    var generatedAt: Instant = Instant.now(),

    @Column(name = "model_version", nullable = false)
    var modelVersion: String
)