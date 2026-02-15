package com.github.charlieboggus.turborecs.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.charlieboggus.turborecs.common.enums.MediaSort
import com.github.charlieboggus.turborecs.common.enums.MediaType
import com.github.charlieboggus.turborecs.config.properties.ClaudeProperties
import com.github.charlieboggus.turborecs.config.properties.TmdbProperties
import com.github.charlieboggus.turborecs.db.repository.MediaItemRepository
import com.github.charlieboggus.turborecs.dto.request.LogMediaRequest
import com.github.charlieboggus.turborecs.dto.response.MediaItemResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class MediaItemService(
    private val mediaItemRepository: MediaItemRepository,
    private val claudeProperties: ClaudeProperties,
    private val tmdbProperties: TmdbProperties,
    private val objectMapper: ObjectMapper
) {

    @Transactional(readOnly = true)
    fun getAllItems(type: MediaType? = null, sortBy: MediaSort? = null): List<MediaItemResponse> {
        val items =
            if (type == null) mediaItemRepository.findAll()
            else mediaItemRepository.findAllByMediaType(type)
        return items
            .map { MediaItemResponse.from(it) }
            .let { list ->
                when (sortBy) {
                    MediaSort.RATING -> list.sortedByDescending { it.rating ?: 0 }
                    MediaSort.TITLE -> list.sortedBy { it.title.lowercase() }
                    MediaSort.DATE_ADDED -> list.sortedByDescending { it.createdAt }
                    null -> list
                }
            }
    }

    @Transactional(readOnly = true)
    fun getAllItemsPaginated(
        type: MediaType? = null,
        page: Int = 0,
        pageSize: Int = 25,
        sortBy: MediaSort? = null
    ): Page<MediaItemResponse> {
        val sort = when (sortBy) {
            MediaSort.RATING -> Sort.by(Sort.Direction.DESC, "rating")
            MediaSort.TITLE -> Sort.by(Sort.Direction.ASC, "title")
            MediaSort.DATE_ADDED -> Sort.by(Sort.Direction.DESC, "createdAt")
            null -> Sort.unsorted()
        }
        val pageable = PageRequest.of(page, pageSize, sort)
        val items =
            if (type == null) mediaItemRepository.findAll(pageable)
            else mediaItemRepository.findAllByMediaType(type, pageable)

        return items.map { MediaItemResponse.from(it) }
    }

    @Transactional(readOnly = true)
    fun getItemById(id: UUID): MediaItemResponse {
        val item = mediaItemRepository
            .findById(id)
            .orElseThrow { NoSuchElementException() }
        return MediaItemResponse.from(item)
    }

    @Transactional
    fun logMediaItem(request: LogMediaRequest): MediaItemResponse {
        if (request.mediaType == MediaType.MOVIE && mediaItemRepository.existsByTmdbId(request.tmdbId!!)) {
            // throw ItemAlreadyExistsException("Movie already in library")
        }
        if (request.mediaType == MediaType.BOOK && mediaItemRepository.existsByOpenLibraryId(request.openLibraryId!!)) {
            // throw ItemAlreadyExistsException("Movie already in library")
        }
    }
}