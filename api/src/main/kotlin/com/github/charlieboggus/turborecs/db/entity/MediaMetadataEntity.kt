package com.github.charlieboggus.turborecs.db.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.util.UUID

@Entity
@Table(name = "media_metadata")
class MediaMetadataEntity(

    @Id
    @Column(name = "media_id", columnDefinition = "uuid")
    var mediaId: UUID? = null,

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "media_id")
    var media: MediaItemEntity,

    @Column(name = "runtime_minutes")
    var runtimeMinutes: Int? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "genres", columnDefinition = "jsonb", nullable = false)
    var genres: MutableList<String> = mutableListOf()
)