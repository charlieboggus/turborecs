package com.github.charlieboggus.turborecs.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.charlieboggus.turborecs.db.entity.BookMetadataEntity
import com.github.charlieboggus.turborecs.db.entity.MediaItemEntity
import com.github.charlieboggus.turborecs.db.entity.MediaMetadataEntity
import com.github.charlieboggus.turborecs.db.entity.WatchHistoryEntity
import com.github.charlieboggus.turborecs.db.entity.enums.MediaType
import com.github.charlieboggus.turborecs.db.repository.BookMetadataRepository
import com.github.charlieboggus.turborecs.db.repository.MediaItemRepository
import com.github.charlieboggus.turborecs.db.repository.MediaMetadataRepository
import com.github.charlieboggus.turborecs.db.repository.WatchHistoryRepository
import com.github.charlieboggus.turborecs.web.dto.MediaItemResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
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

@Service
class EnrichmentService(
    private val mediaItemRepository: MediaItemRepository,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val mediaMetadataRepository: MediaMetadataRepository,
    private val bookMetadataRepository: BookMetadataRepository,
    private val objectMapper: ObjectMapper,
    private val tmdbRestClient: RestClient
) {
    private val log = LoggerFactory.getLogger(EnrichmentService::class.java)

    private val openLibraryClient: RestClient = RestClient.builder()
        .baseUrl("https://openlibrary.org")
        .build()

    /**
     * Enrich a single item. Idempotent-ish: safe to call repeatedly.
     *
     * Important: we avoid holding a DB transaction open across network calls.
     */
    fun enrichItem(mediaId: UUID): MediaItemResponse {
        val snapshot = mediaItemRepository.findById(mediaId).orElseThrow {
            NoSuchElementException("Media item not found: $mediaId")
        }

        val enrichment = when (snapshot.mediaType) {
            MediaType.MOVIE -> fetchMovieEnrichment(snapshot)
            MediaType.BOOK -> fetchBookEnrichment(snapshot)
        }

        val saved = persistEnrichment(mediaId, enrichment)
        val latest = watchHistoryRepository.findFirstByMedia_IdOrderByCreatedAtDesc(mediaId)
        return saved.toResponse(latest)
    }

    /**
     * Returns IDs of items that look unenriched (conservative heuristic).
     */
    @Transactional(readOnly = true)
    fun findUnenrichedIds(limit: Int = 200): List<UUID> =
        mediaItemRepository.findAll()
            .asSequence()
            .mapNotNull { it.id }
            .filter { id ->
                val item = mediaItemRepository.findById(id).orElse(null) ?: return@filter false
                item.creator == null || item.description == null || item.posterUrl == null
            }
            .take(limit)
            .toList()

    fun enrichAllUnenriched(limit: Int = 200): List<MediaItemResponse> {
        val ids = findUnenrichedIds(limit)
        if (ids.isEmpty()) return emptyList()

        log.info("Enriching {} items (limit={})", ids.size, limit)

        val out = ArrayList<MediaItemResponse>(ids.size)
        for (id in ids) {
            try {
                out += enrichItem(id)
            } catch (e: Exception) {
                log.warn("Failed enrichment for mediaId={}: {}", id, e.message)
            }
        }
        return out
    }

    // -----------------------------
    // Fetch phase (NO DB writes)
    // -----------------------------

    private data class MovieEnrichment(
        val tmdbId: String?,
        val director: String?,
        val posterUrl: String?,
        val year: Int?,
        val overview: String?,
        val runtimeMinutes: Int?,
        val genres: List<String>?
    )

    private data class BookEnrichment(
        val openLibraryId: String?,
        val author: String?,
        val posterUrl: String?,
        val year: Int?,
        val description: String?,
        val pageCount: Int?,
        val isbn: String?
    )

    private sealed interface EnrichmentPayload {
        data class Movie(val data: MovieEnrichment) : EnrichmentPayload
        data class Book(val data: BookEnrichment) : EnrichmentPayload
    }

    private fun fetchMovieEnrichment(item: MediaItemEntity): EnrichmentPayload.Movie {
        val existingTmdbId = item.tmdbId?.trim().takeIf { !it.isNullOrBlank() }

        val tmdbId = existingTmdbId ?: run {
            val found = searchTmdbMovieId(title = item.title, year = item.year)
            if (found == null) {
                log.info("TMDB search returned nothing for title='{}' year={}", item.title, item.year)
            }
            found
        }

        if (tmdbId == null) {
            return EnrichmentPayload.Movie(
                MovieEnrichment(
                    tmdbId = existingTmdbId,
                    director = null,
                    posterUrl = null,
                    year = null,
                    overview = null,
                    runtimeMinutes = null,
                    genres = null
                )
            )
        }

        val detailJson = try {
            tmdbRestClient.get()
                .uri("/movie/{id}?append_to_response=credits", tmdbId)
                .retrieve()
                .body(String::class.java)
                ?.let(objectMapper::readTree)
        } catch (e: RestClientResponseException) {
            log.warn(
                "TMDB detail failed (status={}) for tmdbId={}: {}",
                e.statusCode.value(),
                tmdbId,
                e.responseBodyAsString
            )
            null
        } catch (e: Exception) {
            log.warn("TMDB detail failed for tmdbId={}: {}", tmdbId, e.message)
            null
        } ?: return EnrichmentPayload.Movie(
            MovieEnrichment(
                tmdbId = tmdbId,
                director = null,
                posterUrl = null,
                year = null,
                overview = null,
                runtimeMinutes = null,
                genres = null
            )
        )

        val director = detailJson.path("credits").path("crew")
            .firstOrNull { it.path("job").asText() == "Director" }
            ?.path("name")?.asText(null)
            ?.takeIf { !it.isNullOrBlank() }

        val posterUrl = detailJson.path("poster_path")
            .asText(null)
            ?.takeIf { it.isNotBlank() && it != "null" }
            ?.let { "https://image.tmdb.org/t/p/w500$it" }

        val runtimeMinutes = detailJson.path("runtime")
            .takeIf { it.isInt || it.isNumber }
            ?.asInt()

        val yearFromRelease = detailJson.path("release_date").asText(null)
            ?.take(4)
            ?.toIntOrNull()

        val overview = detailJson.path("overview").asText(null)
            ?.takeIf { !it.isNullOrBlank() && it != "null" }

        val genres: List<String> = detailJson.path("genres")
            .takeIf { it.isArray }
            ?.mapNotNull { it.path("name").asText(null)?.trim() }
            ?.filter { it.isNotBlank() }
            ?: emptyList()

        return EnrichmentPayload.Movie(
            MovieEnrichment(
                tmdbId = tmdbId,
                director = director,
                posterUrl = posterUrl,
                year = yearFromRelease,
                overview = overview,
                runtimeMinutes = runtimeMinutes,
                genres = genres
            )
        )
    }

    private fun searchTmdbMovieId(title: String, year: Int?): String? {
        val q = title.trim()
        if (q.isEmpty()) return null

        val encoded = URLEncoder.encode(q, StandardCharsets.UTF_8)
        val uri = if (year != null) "/search/movie?query=$encoded&year=$year" else "/search/movie?query=$encoded"

        val json = try {
            tmdbRestClient.get()
                .uri(uri)
                .retrieve()
                .body(String::class.java)
                ?.let(objectMapper::readTree)
        } catch (e: Exception) {
            log.warn("TMDB search failed for title='{}' year={}: {}", title, year, e.message)
            null
        } ?: return null

        val results = json.path("results")
        if (!results.isArray || results.isEmpty) return null

        if (year != null) {
            val match = results.firstOrNull { node ->
                node.path("release_date").asText("").take(4).toIntOrNull() == year
            }
            if (match != null) return match.path("id").asText(null)
        }

        return results[0].path("id").asText(null)
    }

    private fun fetchBookEnrichment(item: MediaItemEntity): EnrichmentPayload.Book {
        val existingOlid = item.openLibraryId?.trim().takeIf { !it.isNullOrBlank() }

        val json = try {
            openLibraryClient.get()
                .uri("/search.json?title={title}&limit=1", item.title)
                .retrieve()
                .body(String::class.java)
                ?.let(objectMapper::readTree)
        } catch (e: Exception) {
            log.warn("OpenLibrary search failed for title='{}': {}", item.title, e.message)
            null
        } ?: return EnrichmentPayload.Book(
            BookEnrichment(
                openLibraryId = existingOlid,
                author = null,
                posterUrl = null,
                year = null,
                description = null,
                pageCount = null,
                isbn = null
            )
        )

        val docs = json.path("docs")
        if (!docs.isArray || docs.isEmpty) {
            return EnrichmentPayload.Book(
                BookEnrichment(
                    openLibraryId = existingOlid,
                    author = null,
                    posterUrl = null,
                    year = null,
                    description = null,
                    pageCount = null,
                    isbn = null
                )
            )
        }

        val doc = docs[0]

        val coverId = doc.path("cover_i").takeIf { it.isInt }?.asInt()
        val posterUrl = coverId?.let { "https://covers.openlibrary.org/b/id/$it-L.jpg" }

        val author = doc.path("author_name").takeIf { it.isArray }?.firstOrNull()?.asText(null)
            ?.takeIf { it.isNotBlank() }

        val year = doc.path("first_publish_year").takeIf { it.isInt }?.asInt()

        val pageCount = doc.path("number_of_pages_median").takeIf { it.isInt }?.asInt()

        val firstSentence = doc.path("first_sentence").takeIf { it.isArray }?.firstOrNull()?.asText(null)
            ?.takeIf { it.isNotBlank() }

        val isbn = doc.path("isbn").takeIf { it.isArray }?.firstOrNull()?.asText(null)
            ?.takeIf { it.isNotBlank() }

        return EnrichmentPayload.Book(
            BookEnrichment(
                openLibraryId = existingOlid,
                author = author,
                posterUrl = posterUrl,
                year = year,
                description = firstSentence,
                pageCount = pageCount,
                isbn = isbn
            )
        )
    }

    // -----------------------------
    // Persist phase (DB writes)
    // -----------------------------

    @Transactional
    private fun persistEnrichment(mediaId: UUID, payload: EnrichmentPayload): MediaItemEntity {
        val item = mediaItemRepository.findById(mediaId).orElseThrow {
            NoSuchElementException("Media item not found: $mediaId")
        }

        when (payload) {
            is EnrichmentPayload.Movie -> applyMovieEnrichment(item, payload.data)
            is EnrichmentPayload.Book -> applyBookEnrichment(item, payload.data)
        }

        item.updatedAt = Instant.now()
        return mediaItemRepository.save(item)
    }

    private fun applyMovieEnrichment(item: MediaItemEntity, e: MovieEnrichment) {
        val id = requireNotNull(item.id)

        // fill-if-null to avoid clobbering user edits
        item.tmdbId = item.tmdbId ?: e.tmdbId
        item.creator = item.creator ?: e.director
        item.posterUrl = item.posterUrl ?: e.posterUrl
        item.year = item.year ?: e.year
        item.description = item.description ?: e.overview

        val existing = mediaMetadataRepository.findById(id).orElse(null)
        if (existing != null) {
            if (existing.runtimeMinutes == null) existing.runtimeMinutes = e.runtimeMinutes
            if (existing.genres.isEmpty() && !e.genres.isNullOrEmpty()) existing.genres = e.genres as MutableList<String>
            mediaMetadataRepository.save(existing)
        } else {
            // IMPORTANT: for @MapsId, set media=item and let Hibernate derive the id from the association
            mediaMetadataRepository.save(
                MediaMetadataEntity(
                    mediaId = null,
                    media = item,
                    runtimeMinutes = e.runtimeMinutes,
                    genres = (e.genres ?: emptyList()) as MutableList<String>
                )
            )
        }
    }

    private fun applyBookEnrichment(item: MediaItemEntity, e: BookEnrichment) {
        val id = requireNotNull(item.id)

        item.openLibraryId = item.openLibraryId ?: e.openLibraryId
        item.creator = item.creator ?: e.author
        item.posterUrl = item.posterUrl ?: e.posterUrl
        item.year = item.year ?: e.year
        item.description = item.description ?: e.description

        val existing = bookMetadataRepository.findById(id).orElse(null)
        if (existing != null) {
            if (existing.pageCount == null) existing.pageCount = e.pageCount
            if (existing.isbn == null) existing.isbn = e.isbn
            bookMetadataRepository.save(existing)
        } else {
            bookMetadataRepository.save(
                BookMetadataEntity(
                    mediaId = null,
                    media = item,
                    pageCount = e.pageCount,
                    isbn = e.isbn,
                    publisher = null
                )
            )
        }
    }
}

private fun JsonNode.firstOrNull(predicate: (JsonNode) -> Boolean): JsonNode? {
    if (!this.isArray) return null
    for (n in this) if (predicate(n)) return n
    return null
}