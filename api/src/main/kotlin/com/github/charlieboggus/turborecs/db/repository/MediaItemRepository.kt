package com.github.charlieboggus.turborecs.db.repository

import com.github.charlieboggus.turborecs.db.entity.MediaItemEntity
import com.github.charlieboggus.turborecs.db.entity.enums.MediaType
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MediaItemRepository : JpaRepository<MediaItemEntity, UUID> {
    fun findByTmdbId(tmdbId: String): MediaItemEntity?
    fun findByOpenLibraryId(openLibraryId: String): MediaItemEntity?
    fun findAllByMediaType(mediaType: MediaType): List<MediaItemEntity>
    fun findAllByTitleContainingIgnoreCase(title: String): List<MediaItemEntity>
}