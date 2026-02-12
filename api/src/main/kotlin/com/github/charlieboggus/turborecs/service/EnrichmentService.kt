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
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClient
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
    @Value("\${turborecs.tmdb.api.key}") private val tmdbApiKey: String
) {
    private val log = LoggerFactory.getLogger(EnrichmentService::class.java)

    private val tmdbClient = RestClient.builder()
        .baseUrl("https://api.themoviedb.org/3")
        // expects TMDB Read Access Token (Bearer). If using v3 key, change auth approach.
        .defaultHeader("Authorization", "Bearer $tmdbApiKey")
        .build()

    private val openLibraryClient = RestClient.builder()
        .baseUrl("https://openlibrary.org")
        .build()

    @Transactional
    fun enrichItem(mediaId: UUID): MediaItemResponse {
        val item = mediaItemRepository.findById(mediaId).orElseThrow {
            NoSuchElementException("Media item not found: $mediaId")
        }

        val enriched = when (item.mediaType) {
            MediaType.MOVIE -> enrichMovie(item)
            MediaType.BOOK -> enrichBook(item)
        }

        val savedItem = mediaItemRepository.save(enriched)
        val latest = watchHistoryRepository.findFirstByMedia_IdOrderByCreatedAtDesc(mediaId)
        return savedItem.toResponse(latest)
    }

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

        val results = mutableListOf<MediaItemResponse>()
        for (id in ids) {
            try {
                results += enrichItem(id)
            } catch (e: Exception) {
                log.warn("Failed to enrich {}: {}", id, e.message)
            }
        }
        return results
    }

    private fun enrichMovie(item: MediaItemEntity): MediaItemEntity {
        val id = requireNotNull(item.id)
        val tmdbId = item.tmdbId?.trim().takeIf { !it.isNullOrBlank() } ?: return item

        val detailJson = tmdbClient.get()
            .uri("/movie/{id}?append_to_response=credits", tmdbId)
            .retrieve()
            .body(String::class.java)
            ?.let(objectMapper::readTree)
            ?: return item

        val director = detailJson.path("credits").path("crew")
            .firstOrNull { it.path("job").asText() == "Director" }
            ?.path("name")?.asText()
            ?.takeIf { it.isNotBlank() }

        val posterUrl = detailJson.path("poster_path")
            .asText(null)
            ?.takeIf { it.isNotBlank() && it != "null" }
            ?.let { "https://image.tmdb.org/t/p/w500$it" }

        val genres: List<String> = detailJson.path("genres")
            .takeIf { it.isArray }
            ?.mapNotNull { it.path("name").asText(null) }
            ?.filter { it.isNotBlank() }
            ?: emptyList()

        val runtimeMinutes = detailJson.path("runtime").takeIf { it.isInt }?.asInt()

        val yearFromRelease = detailJson.path("release_date").asText(null)
            ?.take(4)
            ?.toIntOrNull()

        val overview = detailJson.path("overview").asText(null)
            ?.takeIf { it.isNotBlank() && it != "null" }

        val genresJson = objectMapper.writeValueAsString(genres)
        val existing = mediaMetadataRepository.findById(id).orElse(null)
        if (existing != null) {
            existing.runtimeMinutes = runtimeMinutes
            existing.genresJson = genresJson
            // existing.media is already set via JPA
            mediaMetadataRepository.save(existing)
        } else {
            mediaMetadataRepository.save(
                MediaMetadataEntity(
                    mediaId = id,
                    media = item,
                    runtimeMinutes = runtimeMinutes,
                    genresJson = genresJson
                )
            )
        }

        // Update media_items fields (fill-if-null)
        item.creator = item.creator ?: director
        item.posterUrl = item.posterUrl ?: posterUrl
        item.year = item.year ?: yearFromRelease
        item.description = item.description ?: overview
        item.updatedAt = Instant.now()

        return item
    }

    private fun enrichBook(item: MediaItemEntity): MediaItemEntity {
        val id = requireNotNull(item.id)
        val olid = item.openLibraryId?.trim().takeIf { !it.isNullOrBlank() } ?: return item

        val path = when {
            olid.endsWith("W") -> "/works/$olid.json"
            olid.endsWith("M") -> "/books/$olid.json"
            else -> "/works/$olid.json"
        }

        val json = openLibraryClient.get()
            .uri(path)
            .retrieve()
            .body(String::class.java)
            ?.let(objectMapper::readTree)
            ?: return item

        val coverId = json.path("covers").takeIf { it.isArray }?.firstOrNull()?.asInt()
        val posterUrl = coverId?.let { "https://covers.openlibrary.org/b/id/$it-L.jpg" }

        val description = extractOpenLibraryDescription(json)
        val authorName = resolveFirstAuthorNameIfEasy(json)
        val isbn = extractIsbnIfPresent(json)

        val pageCount = json.path("number_of_pages").takeIf { it.isInt }?.asInt()
            ?: json.path("number_of_pages_median").takeIf { it.isInt }?.asInt()

        val existing = bookMetadataRepository.findById(id).orElse(null)
        if (existing != null) {
            existing.pageCount = pageCount
            existing.isbn = isbn
            bookMetadataRepository.save(existing)
        } else {
            bookMetadataRepository.save(
                BookMetadataEntity(
                    mediaId = id,
                    media = item,
                    pageCount = pageCount,
                    isbn = isbn,
                    publisher = null
                )
            )
        }

        // Update media_items fields (fill-if-null)
        item.posterUrl = item.posterUrl ?: posterUrl
        item.description = item.description ?: description
        item.creator = item.creator ?: authorName
        item.updatedAt = Instant.now()

        return item
    }

    private fun extractOpenLibraryDescription(json: JsonNode): String? {
        val desc = json.get("description") ?: return null
        return when {
            desc.isTextual -> desc.asText().takeIf { it.isNotBlank() }
            desc.isObject -> desc.get("value")?.asText()?.takeIf { it.isNotBlank() }
            else -> null
        }
    }

    private fun resolveFirstAuthorNameIfEasy(json: JsonNode): String? {
        val authorKey = json.path("authors")
            .takeIf { it.isArray }
            ?.firstOrNull()
            ?.path("author")
            ?.path("key")
            ?.asText(null)
            ?.takeIf { it.isNotBlank() }
            ?: return null

        val authorJson = openLibraryClient.get()
            .uri("$authorKey.json")
            .retrieve()
            .body(String::class.java)
            ?.let(objectMapper::readTree)
            ?: return null

        return authorJson.path("name").asText(null)?.takeIf { it.isNotBlank() }
    }

    private fun extractIsbnIfPresent(json: JsonNode): String? {
        val isbn13 = json.path("isbn_13").takeIf { it.isArray }?.firstOrNull()?.asText(null)
        val isbn10 = json.path("isbn_10").takeIf { it.isArray }?.firstOrNull()?.asText(null)
        return isbn13 ?: isbn10
    }
}