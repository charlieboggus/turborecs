package com.github.charlieboggus.turborecs.service

import com.github.charlieboggus.turborecs.config.properties.ClaudeProperties
import com.github.charlieboggus.turborecs.db.entity.enums.MediaStatus
import com.github.charlieboggus.turborecs.db.entity.enums.MediaType
import com.github.charlieboggus.turborecs.db.repository.MediaMetadataRepository
import com.github.charlieboggus.turborecs.db.repository.MediaTagRepository
import com.github.charlieboggus.turborecs.web.dto.MediaFiltersResponse
import com.github.charlieboggus.turborecs.web.dto.TagSummaryDto
import org.springframework.stereotype.Service

@Service
class MediaFiltersService(
    private val mediaMetadataRepository: MediaMetadataRepository,
    private val mediaTagRepository: MediaTagRepository,
    private val claudeProperties: ClaudeProperties
) {
    fun getFilters(modelVersion: String?): MediaFiltersResponse {
        val mv = modelVersion ?: claudeProperties.model

        val genres = runCatching { mediaMetadataRepository.findDistinctGenres() }.getOrElse { emptyList() }

        val topTags = mediaTagRepository.findPopularTags(modelVersion = mv, limit = 50)
            .map { TagSummaryDto(category = it.getCategory(), name = it.getName(), count = it.getCnt()) }

        return MediaFiltersResponse(
            types = MediaType.entries,
            statuses = MediaStatus.entries,
            genres = genres,
            topTags = topTags
        )
    }
}