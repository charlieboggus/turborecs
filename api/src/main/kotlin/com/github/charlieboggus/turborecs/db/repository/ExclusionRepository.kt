package com.github.charlieboggus.turborecs.db.repository

import com.github.charlieboggus.turborecs.common.enums.MediaType
import com.github.charlieboggus.turborecs.db.entities.ExclusionEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ExclusionRepository : JpaRepository<ExclusionEntity, UUID> {

    // Recommendation engine checks these to filter out excluded titles
    fun existsByTmdbId(tmdbId: String): Boolean

    fun existsByOpenLibraryId(openLibraryId: String): Boolean

    // Load all for building the exclusion set to pass to Claude
    fun findAllByMediaType(mediaType: MediaType): List<ExclusionEntity>
}