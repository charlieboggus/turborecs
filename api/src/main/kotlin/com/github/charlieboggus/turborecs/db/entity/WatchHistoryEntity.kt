package com.github.charlieboggus.turborecs.db.entity

import com.github.charlieboggus.turborecs.db.entity.enums.MediaStatus
import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "watch_history")
class WatchHistoryEntity(
    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "uuid")
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "media_id", nullable = false)
    var media: MediaItemEntity,

    @Column(name = "watched_at", nullable = false)
    var watchedAt: LocalDate,

    @Column(name = "rating")
    var rating: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: MediaStatus,

    @Column(name = "notes")
    var notes: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
)