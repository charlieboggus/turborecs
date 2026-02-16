package com.github.charlieboggus.turborecs.jobs

import com.github.charlieboggus.turborecs.db.repository.TagRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OrphanedTagCleanup(
    private val tagRepository: TagRepository
) {
    private val log = LoggerFactory.getLogger(OrphanedTagCleanup::class.java)

    @Scheduled(cron = "0 0 3 * * *") // daily at 3am
    @Transactional
    fun cleanup() {
        val deleted = tagRepository.deleteOrphanedTags()
        if (deleted > 0) {
            log.info("Cleaned up {} orphaned tags", deleted)
        }
    }
}