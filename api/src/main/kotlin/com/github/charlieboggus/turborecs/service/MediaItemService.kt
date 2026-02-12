package com.github.charlieboggus.turborecs.service

import com.github.charlieboggus.turborecs.db.entity.MediaItemEntity
import com.github.charlieboggus.turborecs.db.entity.WatchHistoryEntity
import com.github.charlieboggus.turborecs.db.entity.enums.MediaStatus
import com.github.charlieboggus.turborecs.db.entity.enums.MediaType
import com.github.charlieboggus.turborecs.db.repository.MediaItemRepository
import com.github.charlieboggus.turborecs.db.repository.WatchHistoryRepository
import com.github.charlieboggus.turborecs.service.enums.MediaSort
import com.github.charlieboggus.turborecs.web.dto.CreateBookRequest
import com.github.charlieboggus.turborecs.web.dto.CreateMovieRequest
import com.github.charlieboggus.turborecs.web.dto.MediaItemResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

private fun MediaItemEntity.toResponse(latest: WatchHistoryEntity?): MediaItemResponse = MediaItemResponse(
    id = requireNotNull(this.id),
    type = this.mediaType,
    title = this.title,
    year = this.year,
    creator = this.creator,
    description = this.description,
    posterUrl = this.posterUrl,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt,
    latestStatus = latest?.status,
    latestRating = latest?.rating
)

private fun defaultWantStatus(type: MediaType): MediaStatus =
    if (type == MediaType.MOVIE) MediaStatus.WANT_TO_WATCH else MediaStatus.WANT_TO_READ

private fun isCompletedStatus(status: MediaStatus): Boolean =
    status == MediaStatus.WATCHED || status == MediaStatus.FINISHED

@Service
class MediaItemService(
    private val mediaItemRepository: MediaItemRepository,
    private val watchHistoryRepository: WatchHistoryRepository
) {

    @Transactional(readOnly = true)
    fun getAllItems(
        type: MediaType? = null,
        status: MediaStatus? = null,
        sortBy: MediaSort? = null,
    ): List<MediaItemResponse> {
        val items: List<MediaItemEntity> =
            if (type == null) mediaItemRepository.findAll()
            else mediaItemRepository.findAllByMediaType(type)

        if (items.isEmpty()) return emptyList()

        val mediaIds = items.mapNotNull { it.id }
        val latestByMediaId = watchHistoryRepository
            .findLatestForMediaIds(mediaIds)
            .associateBy { requireNotNull(it.media.id) }
        val filtered: List<Pair<MediaItemEntity, WatchHistoryEntity?>> = items
            .map { it to latestByMediaId[it.id] }
            .let { pairs ->
                if (status == null) pairs else pairs.filter { (_, latest) -> latest?.status == status }
            }
        val sorted = when (sortBy) {
            MediaSort.RATING -> filtered.sortedByDescending { (_, latest) -> latest?.rating ?: 0 }
            MediaSort.TITLE -> filtered.sortedBy { (item, _) -> item.title.lowercase() }
            MediaSort.DATE_ADDED -> filtered.sortedByDescending { (item, _) -> item.createdAt }
            else -> filtered
        }
        return sorted.map { (item, latest) -> item.toResponse(latest) }
    }

    @Transactional(readOnly = true)
    fun getItemById(id: UUID): MediaItemResponse {
        val item = mediaItemRepository
            .findById(id)
            .orElseThrow {
                NoSuchElementException("Media item not found: $id")
            }
        val latest = watchHistoryRepository.findFirstByMedia_IdOrderByCreatedAtDesc(id)
        return item.toResponse(latest)
    }

    @Transactional
    fun createMovie(req: CreateMovieRequest): MediaItemResponse {

        require(req.tmdbId.isNotBlank()) { "tmdbId must not be blank" }
        require(req.title.isNotBlank()) { "title must not be blank" }

        mediaItemRepository.findByTmdbId(req.tmdbId)?.let {
            throw IllegalArgumentException("MOVIE with tmdbId='${req.tmdbId}' already exists (id=${it.id})")
        }

        val saved = mediaItemRepository.save(
            MediaItemEntity(
                id = null, // DB generates
                tmdbId = req.tmdbId,
                openLibraryId = null,
                title = req.title.trim(),
                mediaType = MediaType.MOVIE,
                year = req.year,
                creator = req.creator?.trim(),
                description = req.description,
                posterUrl = req.posterUrl,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        )
        val history = watchHistoryRepository.save(
            WatchHistoryEntity(
                id = null,
                media = saved,
                watchedAt = LocalDate.now(), // date the item was added to your list
                rating = null,
                status = MediaStatus.WANT_TO_WATCH,
                notes = null,
                createdAt = Instant.now()
            )
        )
        return saved.toResponse(history)
    }

    @Transactional
    fun createBook(req: CreateBookRequest): MediaItemResponse {

        require(req.openLibraryId.isNotBlank()) { "openLibraryId must not be blank" }
        require(req.title.isNotBlank()) { "title must not be blank" }

        mediaItemRepository.findByOpenLibraryId(req.openLibraryId)?.let {
            throw IllegalArgumentException("BOOK with openLibraryId='${req.openLibraryId}' already exists (id=${it.id})")
        }

        val saved = mediaItemRepository.save(
            MediaItemEntity(
                id = null, // DB generates
                tmdbId = null,
                openLibraryId = req.openLibraryId,
                title = req.title.trim(),
                mediaType = MediaType.BOOK,
                year = req.year,
                creator = req.creator?.trim(),
                description = req.description,
                posterUrl = req.posterUrl,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        )
        val history = watchHistoryRepository.save(
            WatchHistoryEntity(
                id = null,
                media = saved,
                watchedAt = LocalDate.now(), // date the item was added to your list
                rating = null,
                status = MediaStatus.WANT_TO_READ,
                notes = null,
                createdAt = Instant.now()
            )
        )
        return saved.toResponse(history)
    }

    @Transactional
    fun updateStatus(mediaId: UUID, status: MediaStatus) {
        val item = mediaItemRepository
            .findById(mediaId)
            .orElseThrow {
                NoSuchElementException("Media item not found: $mediaId")
            }
        val latest = watchHistoryRepository.findFirstByMedia_IdOrderByCreatedAtDesc(mediaId)
        // watchedAt only changes when status becomes WATCHED/FINISHED
        val watchedAt =
            if (isCompletedStatus(status)) LocalDate.now()
            else latest?.watchedAt ?: LocalDate.now()

        watchHistoryRepository.save(
            WatchHistoryEntity(
                id = null,
                media = item,
                watchedAt = watchedAt,
                rating = latest?.rating,
                status = status,
                notes = latest?.notes,
                createdAt = Instant.now()
            )
        )

        item.updatedAt = Instant.now()
        mediaItemRepository.save(item)
    }

    @Transactional
    fun rateItem(mediaId: UUID, rating: Int) {

        require(rating in 1..5) { "Rating must be between 1 and 5" }

        val item = mediaItemRepository
            .findById(mediaId)
            .orElseThrow {
                NoSuchElementException("Media item not found: $mediaId")
            }
        val latest = watchHistoryRepository.findFirstByMedia_IdOrderByCreatedAtDesc(mediaId)
        watchHistoryRepository.save(
            WatchHistoryEntity(
                id = null,
                media = item,
                watchedAt = latest?.watchedAt ?: LocalDate.now(),
                rating = rating,
                status = latest?.status ?: defaultWantStatus(item.mediaType),
                notes = latest?.notes,
                createdAt = Instant.now()
            )
        )
        item.updatedAt = Instant.now()
        mediaItemRepository.save(item)
    }

    @Transactional
    fun updateNotes(mediaId: UUID, notes: String?) {
        val item = mediaItemRepository
            .findById(mediaId)
            .orElseThrow {
                NoSuchElementException("Media item not found: $mediaId")
            }
        val latest = watchHistoryRepository.findFirstByMedia_IdOrderByCreatedAtDesc(mediaId)
        watchHistoryRepository.save(
            WatchHistoryEntity(
                id = null,
                media = item,
                watchedAt = latest?.watchedAt ?: LocalDate.now(),
                rating = latest?.rating,
                status = latest?.status ?: defaultWantStatus(item.mediaType),
                notes = notes,
                createdAt = Instant.now()
            )
        )
        item.updatedAt = Instant.now()
        mediaItemRepository.save(item)
    }

    @Transactional(readOnly = true)
    fun searchByTitle(query: String): List<MediaItemResponse> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()

        val items = mediaItemRepository.findAllByTitleContainingIgnoreCase(q)
        if (items.isEmpty()) return emptyList()

        val mediaIds = items.mapNotNull { it.id }
        val latestByMediaId = watchHistoryRepository
            .findLatestForMediaIds(mediaIds)
            .associateBy { requireNotNull(it.media.id) }

        return items.map { item ->
            item.toResponse(latestByMediaId[item.id])
        }
    }

    @Transactional
    fun deleteItem(mediaId: UUID) {
        if (!mediaItemRepository.existsById(mediaId)) {
            throw NoSuchElementException("Media item not found: $mediaId")
        }
        mediaItemRepository.deleteById(mediaId)
    }
}