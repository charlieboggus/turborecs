package com.github.charlieboggus.turborecs.service

import com.github.charlieboggus.turborecs.config.properties.ClaudeProperties
import com.github.charlieboggus.turborecs.db.entity.enums.MediaStatus
import com.github.charlieboggus.turborecs.db.entity.enums.MediaType
import com.github.charlieboggus.turborecs.db.repository.BookMetadataRepository
import com.github.charlieboggus.turborecs.db.repository.MediaItemRepository
import com.github.charlieboggus.turborecs.db.repository.MediaMetadataRepository
import com.github.charlieboggus.turborecs.db.repository.MediaTagRepository
import com.github.charlieboggus.turborecs.db.repository.WatchHistoryRepository
import com.github.charlieboggus.turborecs.web.dto.AdminStatsResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminService(
    private val mediaItemRepository: MediaItemRepository,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val mediaTagRepository: MediaTagRepository,
    private val mediaMetadataRepository: MediaMetadataRepository,
    private val bookMetadataRepository: BookMetadataRepository,
    private val claudeProperties: ClaudeProperties
) {
    @Transactional(readOnly = true)
    fun stats(modelVersion: String?): AdminStatsResponse {
        val mv = modelVersion ?: claudeProperties.model
        val total = mediaItemRepository.count()
        val itemsByType = MediaType.entries.associateWith { t ->
            mediaItemRepository.countByMediaType(t)
        }
        val ids = mediaItemRepository.findAllIds()
        val latest = if (ids.isEmpty()) emptyList() else watchHistoryRepository.findLatestForMediaIds(ids)
        val latestStatusCounts = latest.groupingBy { it.status }.eachCount().mapValues { it.value.toLong() }
            .let { counts ->
                MediaStatus.entries.associateWith { s -> counts[s] ?: 0L }
            }
        val taggedCount = mediaTagRepository.countDistinctTaggedMedia(mv)
        val movieMetaCount = mediaMetadataRepository.count()
        val bookMetaCount = bookMetadataRepository.count()
        return AdminStatsResponse(
            totalItems = total,
            itemsByType = itemsByType,
            latestStatusCounts = latestStatusCounts,
            taggedItemsForModelVersion = taggedCount,
            movieMetadataCount = movieMetaCount,
            bookMetadataCount = bookMetaCount
        )
    }
}