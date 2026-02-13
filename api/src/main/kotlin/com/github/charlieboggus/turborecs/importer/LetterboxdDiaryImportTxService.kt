package com.github.charlieboggus.turborecs.importer

import com.github.charlieboggus.turborecs.db.entity.MediaItemEntity
import com.github.charlieboggus.turborecs.db.entity.WatchHistoryEntity
import com.github.charlieboggus.turborecs.db.entity.enums.MediaStatus
import com.github.charlieboggus.turborecs.db.entity.enums.MediaType
import com.github.charlieboggus.turborecs.db.repository.MediaItemRepository
import com.github.charlieboggus.turborecs.db.repository.WatchHistoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Service
class LetterboxdDiaryImportTxService(
    private val mediaItemRepository: MediaItemRepository,
    private val watchHistoryRepository: WatchHistoryRepository
) {

    /**
     * DB-only work. No network calls in here.
     *
     * Returns:
     *  - mediaId if created
     *  - null if skipped because existing media item already present
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun importOneMovieRow(
        tmdbId: String,
        title: String,
        year: Int?,
        watchedAt: LocalDate,
        rating: Int?
    ): UUID? {
        // Skip if already present (your current behavior)
        val existing = mediaItemRepository.findByTmdbId(tmdbId)
        if (existing != null) {
            return null
        }

        val now = Instant.now()

        val savedMedia = mediaItemRepository.save(
            MediaItemEntity(
                id = null,
                tmdbId = tmdbId,
                openLibraryId = null,
                title = title,
                mediaType = MediaType.MOVIE,
                year = year,
                creator = null,
                description = null,
                posterUrl = null,
                createdAt = now,
                updatedAt = now
            )
        )

        watchHistoryRepository.save(
            WatchHistoryEntity(
                id = null,
                media = savedMedia,
                watchedAt = watchedAt,
                rating = rating,
                status = MediaStatus.WATCHED,
                notes = null,
                createdAt = now
            )
        )

        return requireNotNull(savedMedia.id)
    }
}