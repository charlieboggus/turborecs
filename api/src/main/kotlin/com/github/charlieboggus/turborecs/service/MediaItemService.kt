package com.github.charlieboggus.turborecs.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.charlieboggus.turborecs.config.properties.ClaudeProperties
import com.github.charlieboggus.turborecs.db.entity.MediaItemEntity
import com.github.charlieboggus.turborecs.db.entity.WatchHistoryEntity
import com.github.charlieboggus.turborecs.db.entity.enums.MediaStatus
import com.github.charlieboggus.turborecs.db.entity.enums.MediaType
import com.github.charlieboggus.turborecs.db.entity.enums.TagCategory
import com.github.charlieboggus.turborecs.db.repository.BookMetadataRepository
import com.github.charlieboggus.turborecs.db.repository.MediaItemRepository
import com.github.charlieboggus.turborecs.db.repository.MediaMetadataRepository
import com.github.charlieboggus.turborecs.db.repository.MediaTagRepository
import com.github.charlieboggus.turborecs.db.repository.WatchHistoryRepository
import com.github.charlieboggus.turborecs.service.enums.MediaSort
import com.github.charlieboggus.turborecs.service.events.MediaLoggedEvent
import com.github.charlieboggus.turborecs.web.dto.BookMetadataDto
import com.github.charlieboggus.turborecs.web.dto.BulkNotesRequest
import com.github.charlieboggus.turborecs.web.dto.BulkRatingRequest
import com.github.charlieboggus.turborecs.web.dto.BulkStatusRequest
import com.github.charlieboggus.turborecs.web.dto.CreateBookRequest
import com.github.charlieboggus.turborecs.web.dto.CreateMovieRequest
import com.github.charlieboggus.turborecs.web.dto.MediaItemDetailResponse
import com.github.charlieboggus.turborecs.web.dto.MediaItemResponse
import com.github.charlieboggus.turborecs.web.dto.MovieMetadataDto
import com.github.charlieboggus.turborecs.web.dto.TagWeightDto
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import com.github.charlieboggus.turborecs.web.dto.PageResponse
import com.github.charlieboggus.turborecs.web.dto.UpdateMediaItemRequest
import com.github.charlieboggus.turborecs.web.dto.WatchHistoryDto
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
    private val watchHistoryRepository: WatchHistoryRepository,
    private val mediaMetadataRepository: MediaMetadataRepository,
    private val bookMetadataRepository: BookMetadataRepository,
    private val mediaTagRepository: MediaTagRepository,
    private val claudeProperties: ClaudeProperties,
    private val objectMapper: ObjectMapper,
    private val eventPublisher: ApplicationEventPublisher
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
    fun getAllItemsPaged(
        type: MediaType? = null,
        status: MediaStatus? = null,
        page: Int = 0,
        size: Int = 50
    ): PageResponse<MediaItemResponse> {
        require(page >= 0) { "page must be >= 0" }
        require(size in 1..100) { "size must be between 1 and 100" }
        val pageable = PageRequest.of(page, size, Sort.by("createdAt").descending())
        val itemPage = if (type == null) {
            mediaItemRepository.findAll(pageable)
        } else {
            mediaItemRepository.findAllByMediaType(type, pageable)
        }
        val items = itemPage.content
        if (items.isEmpty()) {
            return PageResponse(
                items = emptyList(),
                page = page,
                size = size,
                totalItems = itemPage.totalElements,
                totalPages = itemPage.totalPages
            )
        }
        val ids = items.mapNotNull { it.id }
        val latestByMediaId = watchHistoryRepository
            .findLatestForMediaIds(ids)
            .associateBy { requireNotNull(it.media.id) }
        val pairs = items
            .map { it to latestByMediaId[it.id] }
            .let { p -> if (status == null) p else p.filter { (_, latest) -> latest?.status == status } }
        val responses = pairs.map { (item, latest) -> item.toResponse(latest) }
        return PageResponse(
            items = responses,
            page = page,
            size = size,
            totalItems = itemPage.totalElements,
            totalPages = itemPage.totalPages
        )
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

    @Transactional(readOnly = true)
    fun getItemDetailById(id: UUID, modelVersion: String?): MediaItemDetailResponse {
        val item = mediaItemRepository.findById(id).orElseThrow {
            NoSuchElementException("Media item not found: $id")
        }

        val latest = watchHistoryRepository.findFirstByMedia_IdOrderByCreatedAtDesc(id)

        val movieMeta = if (item.mediaType == MediaType.MOVIE) {
            val mm = mediaMetadataRepository.findById(id).orElse(null)
            mm?.let {
                MovieMetadataDto(
                    runtimeMinutes = it.runtimeMinutes,
                    genres = it.genres.toList()
                )
            }
        } else null

        val bookMeta = if (item.mediaType == MediaType.BOOK) {
            val bm = bookMetadataRepository.findById(id).orElse(null)
            bm?.let {
                BookMetadataDto(
                    pageCount = it.pageCount,
                    isbn = it.isbn,
                    publisher = it.publisher
                )
            }
        } else null

        val mv = modelVersion ?: claudeProperties.model
        val tags = mediaTagRepository.fetchTagsForMedia(id, mv)
            .mapNotNull { r ->
                val cat = runCatching { TagCategory.valueOf(r.getCategory()) }.getOrNull() ?: return@mapNotNull null
                TagWeightDto(
                    category = cat,
                    name = r.getName(),
                    weight = r.getWeight()
                )
            }

        return MediaItemDetailResponse(
            id = requireNotNull(item.id),
            type = item.mediaType,
            title = item.title,
            year = item.year,
            creator = item.creator,
            description = item.description,
            posterUrl = item.posterUrl,
            createdAt = item.createdAt,
            updatedAt = item.updatedAt,

            latestStatus = latest?.status,
            latestRating = latest?.rating,
            latestNotes = latest?.notes,
            latestWatchedAt = latest?.watchedAt,

            movieMetadata = movieMeta,
            bookMetadata = bookMeta,
            tags = tags
        )
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
        val mediaId = requireNotNull(saved.id)
        eventPublisher.publishEvent(MediaLoggedEvent(mediaId))
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
        val mediaId = requireNotNull(saved.id)
        eventPublisher.publishEvent(MediaLoggedEvent(mediaId))
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

    @Transactional(readOnly = true)
    fun searchByTitlePaged(query: String, page: Int, size: Int): PageResponse<MediaItemResponse> {
        val q = query.trim()
        if (q.isEmpty()) return PageResponse(emptyList(), page, size, 0, 0)

        val pageable = PageRequest.of(page, size)
        val result = mediaItemRepository.findByTitleContainingIgnoreCase(q, pageable)

        val items = result.content
        if (items.isEmpty()) {
            return PageResponse(emptyList(), page, size, result.totalElements, result.totalPages)
        }

        val mediaIds = items.mapNotNull { it.id }
        val latestByMediaId = watchHistoryRepository
            .findLatestForMediaIds(mediaIds)
            .associateBy { requireNotNull(it.media.id) }

        val dtos = items.map { it.toResponse(latestByMediaId[it.id]) }

        return PageResponse(
            items = dtos,
            page = result.number,
            size = result.size,
            totalItems = result.totalElements,
            totalPages = result.totalPages
        )
    }

    @Transactional(readOnly = true)
    fun getHistory(mediaId: UUID): List<WatchHistoryDto> {
        if (!mediaItemRepository.existsById(mediaId)) {
            throw NoSuchElementException("Media item not found: $mediaId")
        }
        return watchHistoryRepository.findAllByMedia_IdOrderByCreatedAtDesc(mediaId)
            .map {
                WatchHistoryDto(
                    id = requireNotNull(it.id),
                    watchedAt = it.watchedAt,
                    rating = it.rating,
                    status = it.status,
                    notes = it.notes,
                    createdAt = it.createdAt
                )
            }
    }

    @Transactional
    fun updateMediaItemCoreFields(id: UUID, req: UpdateMediaItemRequest): MediaItemResponse {
        val item = mediaItemRepository.findById(id).orElseThrow { NoSuchElementException("Media item not found: $id") }

        // apply only provided fields
        req.title?.let {
            val t = it.trim()
            require(t.isNotBlank()) { "title must not be blank" }
            item.title = t
        }
        req.year?.let { item.year = it }
        req.creator?.let { item.creator = it.trim().ifBlank { null } }
        req.description?.let { item.description = it }
        req.posterUrl?.let { item.posterUrl = it.trim().ifBlank { null } }

        item.updatedAt = Instant.now()
        mediaItemRepository.save(item)

        val latest = watchHistoryRepository.findFirstByMedia_IdOrderByCreatedAtDesc(id)
        return item.toResponse(latest)
    }

    @Transactional
    fun bulkUpdateStatus(req: BulkStatusRequest) {
        req.ids.forEach { id -> updateStatus(id, req.status) }
    }

    @Transactional
    fun bulkUpdateRating(req: BulkRatingRequest) {
        req.ids.forEach { id -> rateItem(id, req.rating) }
    }

    @Transactional
    fun bulkUpdateNotes(req: BulkNotesRequest) {
        req.ids.forEach { id -> updateNotes(id, req.notes) }
    }
}