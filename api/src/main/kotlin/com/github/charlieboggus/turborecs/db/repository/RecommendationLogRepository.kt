package com.github.charlieboggus.turborecs.db.repository

import com.github.charlieboggus.turborecs.db.entities.RecommendationLogEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface RecommendationLogRepository : JpaRepository<RecommendationLogEntity, UUID> {

    // Load all recommendations in a batch
    fun findAllByBatchId(batchId: UUID): List<RecommendationLogEntity>

    // Find the most recent batch for cache check
    @Query("""
        SELECT DISTINCT r.batchId FROM RecommendationLogEntity r
        WHERE r.modelVersion = :modelVersion AND r.shownAt > :since
        ORDER BY r.batchId DESC
    """)
    fun findRecentBatchIds(
        @Param("modelVersion") modelVersion: String,
        @Param("since") since: Instant
    ): List<UUID>

    // Get all active (non-expired) fingerprints for dedup
    @Query("""
        SELECT r.fingerprint FROM RecommendationLogEntity r
        WHERE r.modelVersion = :modelVersion AND r.expiresAt > :now
    """)
    fun findActiveFingerprints(
        @Param("modelVersion") modelVersion: String,
        @Param("now") now: Instant
    ): List<String>
}