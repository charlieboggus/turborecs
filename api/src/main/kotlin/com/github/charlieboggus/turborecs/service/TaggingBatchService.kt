package com.github.charlieboggus.turborecs.service

import com.github.charlieboggus.turborecs.config.properties.ClaudeProperties
import com.github.charlieboggus.turborecs.db.repository.MediaTagRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TaggingBatchService(
    private val taggingService: TaggingService,
    private val mediaTagRepository: MediaTagRepository,
    private val claudeProperties: ClaudeProperties
) {
    private val log = LoggerFactory.getLogger(TaggingBatchService::class.java)

    fun tagAllUntagged(limit: Int = 200, modelVersion: String = claudeProperties.model): List<UUID> {
        val ids = mediaTagRepository.findUntaggedMediaIds(modelVersion, limit)
        if (ids.isEmpty()) return emptyList()

        val succeeded = mutableListOf<UUID>()
        for (id in ids) {
            try {
                taggingService.tagItem(id, modelVersion)
                succeeded += id
            } catch (e: Exception) {
                log.warn("Failed tagging mediaId={}: {}", id, e.message)
            }
        }
        return succeeded
    }
}