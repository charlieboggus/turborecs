package com.github.charlieboggus.turborecs.db.repository

import com.github.charlieboggus.turborecs.db.entities.MediaVectorEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface MediaVectorRepository : JpaRepository<MediaVectorEntity, UUID> {

    fun findByMediaItemIdAndModelVersion(mediaItemId: UUID, modelVersion: String): MediaVectorEntity?

    fun findAllByModelVersion(modelVersion: String): List<MediaVectorEntity>

    @Query("""
        SELECT mv FROM MediaVectorEntity mv
        WHERE mv.modelVersion = :modelVersion
        AND mv.mediaItemId IN :mediaItemIds
    """)
    fun findAllByModelVersionAndMediaItemIds(
        modelVersion: String,
        mediaItemIds: Collection<UUID>
    ): List<MediaVectorEntity>

    fun existsByMediaItemIdAndModelVersion(mediaItemId: UUID, modelVersion: String): Boolean

    @Query("""
        SELECT mv.mediaItemId FROM MediaVectorEntity mv
        WHERE mv.modelVersion = :modelVersion
    """)
    fun findMediaItemIdsWithVectors(modelVersion: String): List<UUID>
}