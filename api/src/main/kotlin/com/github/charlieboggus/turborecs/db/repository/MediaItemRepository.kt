package com.github.charlieboggus.turborecs.db.repository

import com.github.charlieboggus.turborecs.common.enums.MediaType
import com.github.charlieboggus.turborecs.common.enums.TaggingStatus
import com.github.charlieboggus.turborecs.db.entities.MediaItemEntity

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MediaItemRepository : JpaRepository<MediaItemEntity, UUID> {

    fun findAllByMediaType(mediaType: MediaType): List<MediaItemEntity>

    fun findAllByMediaType(mediaType: MediaType, pageable: Pageable?): Page<MediaItemEntity>

    fun existsByTmdbId(tmdbId: String): Boolean

    fun existsByOpenLibraryId(openLibraryId: String): Boolean

    fun findByTmdbId(tmdbId: String): MediaItemEntity?

    fun findByOpenLibraryId(openLibraryId: String): MediaItemEntity?

    fun findAllByTaggingStatus(taggingStatus: TaggingStatus): List<MediaItemEntity>

    fun countByMediaType(mediaType: MediaType): Long
}