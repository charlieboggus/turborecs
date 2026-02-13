package com.github.charlieboggus.turborecs.db.repository

import com.github.charlieboggus.turborecs.db.entity.MediaItemEntity
import com.github.charlieboggus.turborecs.db.entity.enums.MediaType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.query.Param
import java.util.UUID

interface MediaItemRepository : JpaRepository<MediaItemEntity, UUID> {

    fun findByTmdbId(tmdbId: String): MediaItemEntity?

    fun findByOpenLibraryId(openLibraryId: String): MediaItemEntity?

    fun findAllByMediaType(mediaType: MediaType): List<MediaItemEntity>

    fun findAllByMediaType(mediaType: MediaType, pageable: Pageable): Page<MediaItemEntity>

    fun findAllByTitleContainingIgnoreCase(title: String): List<MediaItemEntity>

    fun existsByTmdbId(tmdbId: String): Boolean

    fun existsByOpenLibraryId(openLibraryId: String): Boolean

    @Query("select distinct m.title from MediaItemEntity m where m.title is not null")
    fun findAllTitles(): List<String>

    @Query("select distinct m.title from MediaItemEntity m where m.mediaType = :type and m.title is not null")
    fun findAllTitlesByMediaType(@Param("type") type: MediaType): List<String>
}