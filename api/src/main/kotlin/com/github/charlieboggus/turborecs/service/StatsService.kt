package com.github.charlieboggus.turborecs.service

import com.github.charlieboggus.turborecs.common.enums.MediaType
import com.github.charlieboggus.turborecs.config.properties.ClaudeProperties
import com.github.charlieboggus.turborecs.db.repository.ExclusionRepository
import com.github.charlieboggus.turborecs.db.repository.MediaItemRepository
import com.github.charlieboggus.turborecs.db.repository.MediaMetadataRepository
import com.github.charlieboggus.turborecs.db.repository.MediaTagRepository
import com.github.charlieboggus.turborecs.db.repository.MediaVectorRepository
import com.github.charlieboggus.turborecs.db.repository.RecommendationLogRepository
import com.github.charlieboggus.turborecs.db.repository.TagRepository
import com.github.charlieboggus.turborecs.dto.response.StatsResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class StatsService(
    private val mediaItemRepository: MediaItemRepository,
    private val tagRepository: TagRepository,
    private val mediaTagRepository: MediaTagRepository,
    private val recommendationLogRepository: RecommendationLogRepository,
    private val exclusionRepository: ExclusionRepository,
    private val mediaVectorRepository: MediaVectorRepository
) {

    @Transactional(readOnly = true)
    fun getStats(): StatsResponse {
        return StatsResponse(
            totalItems = mediaItemRepository.count(),
            movieCount = mediaItemRepository.countByMediaType(MediaType.MOVIE),
            bookCount = mediaItemRepository.countByMediaType(MediaType.BOOK),
            uniqueTagCount = tagRepository.count(),
            tagAssignmentCount = mediaTagRepository.count(),
            recommendationCount = recommendationLogRepository.count(),
            exclusionCount = exclusionRepository.count(),
            vectorCount = mediaVectorRepository.count()
        )
    }
}