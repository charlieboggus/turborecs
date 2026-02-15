package com.github.charlieboggus.turborecs.db.repository

import com.github.charlieboggus.turborecs.db.entities.RecommendationLogEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

interface RecommendationLogRepository : JpaRepository<RecommendationLogEntity, UUID> {

    // Load the current batch to display on the recommendations page
    fun findAllByBatchId(batchId: UUID): List<RecommendationLogEntity>

    // Get active (non-expired) recommendations for display
    fun findAllByModelVersionAndExpiresAtAfter(modelVersion: String, now: Instant): List<RecommendationLogEntity>

    // Dedup: check if a fingerprint was already recommended and is still active
    fun existsByModelVersionAndFingerprintAndExpiresAtAfter(modelVersion: String, fingerprint: String, now: Instant): Boolean
}