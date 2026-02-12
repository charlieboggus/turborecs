package com.github.charlieboggus.turborecs.db.entity

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "book_metadata")
class BookMetadataEntity(
    @Id
    @Column(name = "media_id", columnDefinition = "uuid")
    var mediaId: UUID,

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "media_id", nullable = false)
    var media: MediaItemEntity,

    @Column(name = "page_count")
    var pageCount: Int? = null,

    @Column(name = "isbn")
    var isbn: String? = null,

    @Column(name = "publisher")
    var publisher: String? = null
)