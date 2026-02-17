package com.github.charlieboggus.turborecs.db.entities

import com.github.charlieboggus.turborecs.common.DimensionVector
import com.github.charlieboggus.turborecs.common.enums.Dimension
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "media_vectors",
    uniqueConstraints = [UniqueConstraint(columnNames = ["media_item_id", "model_version"])]
)
class MediaVectorEntity(

    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "media_item_id", nullable = false)
    val mediaItemId: UUID,

    @Column(name = "model_version", nullable = false)
    val modelVersion: String,

    @Column(name = "emotional_intensity", nullable = false)
    var emotionalIntensity: Double = 0.0,

    @Column(name = "narrative_complexity", nullable = false)
    var narrativeComplexity: Double = 0.0,

    @Column(name = "moral_ambiguity", nullable = false)
    var moralAmbiguity: Double = 0.0,

    @Column(name = "tone_darkness", nullable = false)
    var toneDarkness: Double = 0.0,

    @Column(name = "pacing", nullable = false)
    var pacing: Double = 0.0,

    @Column(name = "humor", nullable = false)
    var humor: Double = 0.0,

    @Column(name = "violence_intensity", nullable = false)
    var violenceIntensity: Double = 0.0,

    @Column(name = "intellectual_depth", nullable = false)
    var intellectualDepth: Double = 0.0,

    @Column(name = "stylistic_boldness", nullable = false)
    var stylisticBoldness: Double = 0.0,

    @Column(name = "intimacy_scale", nullable = false)
    var intimacyScale: Double = 0.0,

    @Column(name = "realism", nullable = false)
    var realism: Double = 0.0,

    @Column(name = "cultural_specificity", nullable = false)
    var culturalSpecificity: Double = 0.0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()

) {
    fun toDimensionVector(): DimensionVector = DimensionVector(
        mapOf(
            Dimension.EMOTIONAL_INTENSITY to emotionalIntensity,
            Dimension.NARRATIVE_COMPLEXITY to narrativeComplexity,
            Dimension.MORAL_AMBIGUITY to moralAmbiguity,
            Dimension.TONE_DARKNESS to toneDarkness,
            Dimension.PACING to pacing,
            Dimension.HUMOR to humor,
            Dimension.VIOLENCE_INTENSITY to violenceIntensity,
            Dimension.INTELLECTUAL_DEPTH to intellectualDepth,
            Dimension.STYLISTIC_BOLDNESS to stylisticBoldness,
            Dimension.INTIMACY_SCALE to intimacyScale,
            Dimension.REALISM to realism,
            Dimension.CULTURAL_SPECIFICITY to culturalSpecificity
        )
    )

    fun applyVector(vec: DimensionVector) {
        emotionalIntensity = vec[Dimension.EMOTIONAL_INTENSITY]
        narrativeComplexity = vec[Dimension.NARRATIVE_COMPLEXITY]
        moralAmbiguity = vec[Dimension.MORAL_AMBIGUITY]
        toneDarkness = vec[Dimension.TONE_DARKNESS]
        pacing = vec[Dimension.PACING]
        humor = vec[Dimension.HUMOR]
        violenceIntensity = vec[Dimension.VIOLENCE_INTENSITY]
        intellectualDepth = vec[Dimension.INTELLECTUAL_DEPTH]
        stylisticBoldness = vec[Dimension.STYLISTIC_BOLDNESS]
        intimacyScale = vec[Dimension.INTIMACY_SCALE]
        realism = vec[Dimension.REALISM]
        culturalSpecificity = vec[Dimension.CULTURAL_SPECIFICITY]
    }
}