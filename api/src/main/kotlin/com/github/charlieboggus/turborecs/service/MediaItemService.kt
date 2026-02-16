package com.github.charlieboggus.turborecs.service

import com.github.charlieboggus.turborecs.client.OpenLibraryClient
import com.github.charlieboggus.turborecs.client.TmdbClient
import com.github.charlieboggus.turborecs.client.dto.openlibrary.EnrichedBookData
import com.github.charlieboggus.turborecs.common.enums.MediaSort
import com.github.charlieboggus.turborecs.common.enums.MediaType
import com.github.charlieboggus.turborecs.db.entities.MediaItemEntity
import com.github.charlieboggus.turborecs.db.entities.MediaMetadataEntity
import com.github.charlieboggus.turborecs.db.repository.MediaItemRepository
import com.github.charlieboggus.turborecs.dto.request.LogMediaRequest
import com.github.charlieboggus.turborecs.dto.response.MediaItemResponse
import com.github.charlieboggus.turborecs.error.ItemAlreadyExistsException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class MediaItemService(
    private val mediaItemRepository: MediaItemRepository,
    private val tmdbClient: TmdbClient,
    private val openLibraryClient: OpenLibraryClient,
    private val taggingService: TaggingService
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
        limit: Int = 25,
        sortBy: MediaSort? = null
    ): Page<MediaItemResponse> {
        val sort = when (sortBy) {
            MediaSort.RATING -> Sort.by(Sort.Direction.DESC, "rating")
            MediaSort.TITLE -> Sort.by(Sort.Direction.ASC, "title")
            MediaSort.DATE_ADDED -> Sort.by(Sort.Direction.DESC, "createdAt")
            null -> Sort.unsorted()
        }
        val pageable = PageRequest.of(page, limit, sort)
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
            throw ItemAlreadyExistsException("Movie already in library")
        }
        if (request.mediaType == MediaType.BOOK && mediaItemRepository.existsByOpenLibraryId(request.openLibraryId!!)) {
            throw ItemAlreadyExistsException("Book already in library")
        }
        val item = when (request.mediaType) {
            MediaType.MOVIE -> buildMovieItem(request)
            MediaType.BOOK -> buildBookItem(request)
        }
        val saved = mediaItemRepository.save(item)
        taggingService.tagItem(saved)
        return MediaItemResponse.from(saved)
    }

    private fun buildMovieItem(request: LogMediaRequest): MediaItemEntity {
        val details = tmdbClient.getMovie(request.tmdbId!!)
        val item = MediaItemEntity(
            mediaType = MediaType.MOVIE,
            title = details.title,
            year = details.year,
            creator = details.director,
            description = details.overview,
            posterUrl = details.posterUrl,
            tmdbId = request.tmdbId,
            rating = request.rating,
            consumedAt = request.consumedAt
        )
        item.metadata = MediaMetadataEntity(
            mediaId = item.id,
            genres = details.genres.map { it.name },
            runtimeMinutes = details.runtime,
            mediaItem = item
        )
        return item
    }

    private fun buildBookItem(request: LogMediaRequest): MediaItemEntity {
        val work = openLibraryClient.getWork(request.openLibraryId!!)
        val editions = openLibraryClient.getEditions(request.openLibraryId)
        val bestEdition = editions.entries
            .filter { it.isEnglish }
            .maxByOrNull { (if (it.isbn != null) 1 else 0) + (if (it.numberOfPages != null) 1 else 0) }
        val enriched = EnrichedBookData.from(work, bestEdition, null)
        val item = MediaItemEntity(
            mediaType = MediaType.BOOK,
            title = enriched.title,
            year = enriched.year,
            creator = enriched.author,
            description = enriched.description,
            posterUrl = enriched.posterUrl,
            openLibraryId = enriched.openLibraryId,
            rating = request.rating,
            consumedAt = request.consumedAt
        )
        item.metadata = MediaMetadataEntity(
            mediaId = item.id,
            genres = enriched.genres,
            pageCount = enriched.pageCount,
            isbn = enriched.isbn,
            publisher = enriched.publisher,
            mediaItem = item
        )
        return item
    }

    @Transactional
    fun rateMediaItem(mediaId: UUID, rating: Int): MediaItemResponse {
        if (rating !in 1..5) {
            throw IllegalArgumentException("Rating must be between 1 and 5")
        }
        val item = mediaItemRepository
            .findById(mediaId)
            .orElseThrow { NoSuchElementException("Media item not found $mediaId") }
        item.rating = rating
        mediaItemRepository.save(item)
        return MediaItemResponse.from(item)
    }

    @Transactional
    fun deleteMediaItem(mediaId: UUID) {
        if (!mediaItemRepository.existsById(mediaId)) {
            throw NoSuchElementException("Media item not found: $mediaId")
        }
        mediaItemRepository.deleteById(mediaId)
    }
}