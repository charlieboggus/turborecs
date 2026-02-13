package com.github.charlieboggus.turborecs.db.entity

import com.github.charlieboggus.turborecs.db.entity.enums.MediaType
import com.github.charlieboggus.turborecs.db.entity.enums.RecommendationSelection
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "recommendations_log")
class RecommendationLogEntity(

    @Id
    @Column(name = "id", columnDefinition = "uuid")
    var id: UUID? = null,

    @Column(name = "model_version", nullable = false)
    var modelVersion: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "selection", nullable = false)
    var selection: RecommendationSelection,

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    var mediaType: MediaType,

    @Column(name = "title", nullable = false)
    var title: String,

    @Column(name = "year")
    var year: Int? = null,

    @Column(name = "creator")
    var creator: String? = null,

    @Column(name = "reason", nullable = false)
    var reason: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "matched_themes", columnDefinition = "jsonb", nullable = false)
    var matchedThemes: MutableList<String> = mutableListOf(),

    @Column(name = "fingerprint", nullable = false)
    var fingerprint: String,

    @Column(name = "batch_id", columnDefinition = "uuid", nullable = false)
    var batchId: UUID,

    @Column(name = "slot", nullable = false)
    var slot: Int,

    @Column(name = "shown_at", nullable = false)
    var shownAt: Instant,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,

    @Column(name = "replaced_by", columnDefinition = "uuid")
    var replacedBy: UUID? = null
)