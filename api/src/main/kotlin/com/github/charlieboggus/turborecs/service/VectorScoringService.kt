package com.github.charlieboggus.turborecs.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.charlieboggus.turborecs.client.ClaudeClient
import com.github.charlieboggus.turborecs.common.DimensionVector
import com.github.charlieboggus.turborecs.common.enums.Dimension
import com.github.charlieboggus.turborecs.config.properties.ClaudeProperties
import com.github.charlieboggus.turborecs.db.entities.MediaItemEntity
import com.github.charlieboggus.turborecs.db.entities.MediaVectorEntity
import com.github.charlieboggus.turborecs.db.repository.MediaVectorRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VectorScoringService(
    private val claudeClient: ClaudeClient,
    private val claudeProperties: ClaudeProperties,
    private val mediaVectorRepository: MediaVectorRepository,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(VectorScoringService::class.java)

    private val systemPrompt = """
        You are a media analysis engine. Score the given media item on exactly 12 dimensions, each 0.0 to 1.0.
        
        Dimensions and rubric:
        EMOTIONAL_INTENSITY:    0.0=detached/cerebral, 1.0=devastating/gut-punch
        NARRATIVE_COMPLEXITY:   0.0=linear/straightforward, 1.0=non-linear/multi-threaded/unreliable narrator
        MORAL_AMBIGUITY:        0.0=clear heroes+villains, 1.0=no clean answers/gray zones
        TONE_DARKNESS:          0.0=light/warm/optimistic, 1.0=bleak/nihilistic/unflinching
        PACING:                 0.0=slow-burn/meditative, 1.0=relentless/propulsive
        HUMOR:                  0.0=dead serious, 1.0=pervasively funny/comedic core
        VIOLENCE_INTENSITY:     0.0=non-violent/gentle, 1.0=graphic/visceral/brutal
        INTELLECTUAL_DEPTH:     0.0=entertainment-first/escapism, 1.0=ideas-driven/demands active thinking
        STYLISTIC_BOLDNESS:     0.0=conventional/invisible craft, 1.0=highly stylized/auteur/experimental
        INTIMACY_SCALE:         0.0=epic scope/grand sweep, 1.0=claustrophobic/personal/interior
        REALISM:                0.0=fantastical/surreal/speculative, 1.0=grounded/naturalistic
        CULTURAL_SPECIFICITY:   0.0=universal/generic setting, 1.0=deep in specific time/place/culture

        Return ONLY a JSON object with exactly these 12 keys mapped to numbers. No markdown, no explanation.
        Example: {"EMOTIONAL_INTENSITY":0.7,"NARRATIVE_COMPLEXITY":0.4,...}
    """.trimIndent()

    /**
     * Score a single media item. Returns the saved entity, or null on failure.
     */
    @Transactional
    fun scoreItem(item: MediaItemEntity): MediaVectorEntity? {
        val modelVersion = claudeProperties.model
        val existing = mediaVectorRepository.findByMediaItemIdAndModelVersion(item.id, modelVersion)
        if (existing != null) {
            log.debug("Vector already exists for item={} model={}", item.id, modelVersion)
            return existing
        }
        val userMessage = buildString {
            append("${item.mediaType.name}: \"${item.title}\"")
            item.creator?.let { append(" by $it") }
            item.year?.let { append(" ($it)") }
            item.description?.let { d ->
                if (d.length > 300) append("\nSynopsis: ${d.take(300)}...")
                else append("\nSynopsis: $d")
            }
        }
        return try {
            val raw = claudeClient.sendMessage(systemPrompt, userMessage)
            val vec = parseVector(raw) ?: return null
            val entity = MediaVectorEntity(
                mediaItemId = item.id,
                modelVersion = modelVersion
            )
            entity.applyVector(vec)
            mediaVectorRepository.save(entity)
        }
        catch (e: Exception) {
            log.error("Failed to score item={}: {}", item.id, e.message)
            null
        }
    }

    private fun parseVector(raw: String): DimensionVector? {
        val cleaned = raw.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```")
            .trim()
        return try {
            @Suppress("UNCHECKED_CAST")
            val map = objectMapper.readValue(cleaned, Map::class.java) as Map<String, Any>
            val scores = mutableMapOf<Dimension, Double>()
            for (d in Dimension.entries) {
                val v = map[d.name]
                val num = when (v) {
                    is Number -> v.toDouble()
                    is String -> v.toDoubleOrNull() ?: 0.0
                    else -> 0.0
                }
                scores[d] = num.coerceIn(0.0, 1.0)
            }
            DimensionVector(scores)
        }
        catch (e: Exception) {
            log.error("Failed to parse vector JSON: {}", e.message)
            null
        }
    }
}