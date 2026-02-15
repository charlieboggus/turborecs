package com.github.charlieboggus.turborecs.db.entities

import jakarta.persistence.*
import java.util.UUID
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(name = "media_metadata")
class MediaMetadataEntity(
    @Id
    @Column(name = "media_id")
    val mediaId: UUID,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "genres", nullable = false, columnDefinition = "jsonb")
    var genres: List<String> = emptyList(),

    @Column(name = "runtime_minutes")
    var runtimeMinutes: Int? = null,

    @Column(name = "page_count")
    var pageCount: Int? = null,

    @Column(name = "isbn")
    var isbn: String? = null,

    @Column(name = "publisher")
    var publisher: String? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "media_id")
    val mediaItem: MediaItemEntity? = null
)