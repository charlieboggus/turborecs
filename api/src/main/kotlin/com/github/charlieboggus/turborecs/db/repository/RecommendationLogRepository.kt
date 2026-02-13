package com.github.charlieboggus.turborecs.db.repository

import com.github.charlieboggus.turborecs.db.entity.RecommendationLogEntity
import com.github.charlieboggus.turborecs.db.entity.enums.RecommendationSelection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface RecommendationLogRepository : JpaRepository<RecommendationLogEntity, UUID> {

    @Query(
        """
        select distinct r.fingerprint
        from RecommendationLogEntity r
        where r.modelVersion = :modelVersion
          and r.expiresAt > :now
        """
    )
    fun findActiveFingerprints(
        @Param("modelVersion") modelVersion: String,
        @Param("now") now: Instant
    ): List<String>

    @Query(
        """
        select r
        from RecommendationLogEntity r
        where r.batchId = :batchId
        order by r.slot asc
        """
    )
    fun findByBatchIdOrderBySlot(@Param("batchId") batchId: UUID): List<RecommendationLogEntity>

    @Query(
        """
        select r.batchId
        from RecommendationLogEntity r
        where r.modelVersion = :modelVersion
          and r.selection = :selection
          and r.shownAt >= :since
        group by r.batchId
        order by max(r.shownAt) desc
        """
    )
    fun findRecentBatchIds(
        @Param("modelVersion") modelVersion: String,
        @Param("selection") selection: RecommendationSelection,
        @Param("since") since: Instant
    ): List<UUID>

    fun findByBatchIdAndReplacedByIsNullOrderBySlot(batchId: UUID): List<RecommendationLogEntity>

    fun findByBatchIdAndSlotAndReplacedByIsNull(batchId: UUID, slot: Int): RecommendationLogEntity?
}