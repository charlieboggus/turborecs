package com.github.charlieboggus.turborecs.db.repository

import com.github.charlieboggus.turborecs.db.entity.WatchHistoryEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface WatchHistoryRepository : JpaRepository<WatchHistoryEntity, UUID> {

    // “Latest” by created_at makes the most sense for “current status”
    fun findFirstByMedia_IdOrderByCreatedAtDesc(mediaId: UUID): WatchHistoryEntity?

    fun findAllByMedia_IdOrderByWatchedAtDesc(mediaId: UUID): List<WatchHistoryEntity>

    @Query("""
        select wh
        from WatchHistoryEntity wh
        where wh.media.id in :mediaIds
            and wh.createdAt = (
                select max(wh2.createdAt)
                from WatchHistoryEntity wh2
                where wh2.media.id = wh.media.id
            )
    """)
    fun findLatestForMediaIds(@Param("mediaIds") mediaIds: List<UUID>): List<WatchHistoryEntity>

    fun findAllByMedia_IdOrderByCreatedAtDesc(mediaId: UUID): List<WatchHistoryEntity>
}