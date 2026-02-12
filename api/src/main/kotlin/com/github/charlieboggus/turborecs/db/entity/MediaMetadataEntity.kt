package com.github.charlieboggus.turborecs.db.entity

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "media_metadata")
class MediaMetadataEntity(
    @Id
    @Column(name = "media_id", columnDefinition = "uuid")
    var mediaId: UUID,

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "media_id", nullable = false)
    var media: MediaItemEntity,

    @Column(name = "runtime_minutes")
    var runtimeMinutes: Int? = null,

    /**
     * Stored as JSONB in Postgres; keep as String for v1 simplicity.
     * Example: '["Fantasy","Sci-Fi"]'
     */
    @Column(name = "genres", nullable = false, columnDefinition = "jsonb")
    var genresJson: String = "[]"
)