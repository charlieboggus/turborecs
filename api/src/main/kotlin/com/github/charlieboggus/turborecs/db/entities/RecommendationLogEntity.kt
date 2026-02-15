package com.github.charlieboggus.turborecs.db.entities

import com.github.charlieboggus.turborecs.common.enums.MediaType

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "recommendations_log")
class RecommendationLogEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "batch_id", nullable = false)
    val batchId: UUID,

    @Column(name = "slot", nullable = false)
    val slot: Int,

    @Column(name = "model_version", nullable = false)
    val modelVersion: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    val mediaType: MediaType,

    @Column(name = "title", nullable = false)
    val title: String,

    @Column(name = "year")
    val year: Int? = null,

    @Column(name = "creator")
    val creator: String? = null,

    @Column(name = "reason", nullable = false)
    val reason: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "matched_tags", nullable = false, columnDefinition = "jsonb")
    val matchedTags: List<String> = emptyList(),

    @Column(name = "fingerprint", nullable = false)
    val fingerprint: String,

    @Column(name = "shown_at", nullable = false)
    val shownAt: Instant,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,

    @Column(name = "replaced_by")
    var replacedBy: UUID? = null
)