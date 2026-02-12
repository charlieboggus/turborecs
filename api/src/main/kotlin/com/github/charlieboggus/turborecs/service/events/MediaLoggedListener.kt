package com.github.charlieboggus.turborecs.service.events

import com.github.charlieboggus.turborecs.service.EnrichmentService
import com.github.charlieboggus.turborecs.service.TaggingService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.transaction.event.TransactionPhase

@Component
class MediaLoggedListener(
    private val enrichmentService: EnrichmentService,
    private val taggingService: TaggingService
) {
    private val log = LoggerFactory.getLogger(MediaLoggedListener::class.java)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onMediaLogged(event: MediaLoggedEvent) {
        val id = event.mediaId

        // Optional: enrichment first (to improve tagging quality)
        runCatching { enrichmentService.enrichItem(id) }
            .onFailure { log.warn("Enrichment failed for {}: {}", id, it.message) }

        // Required: tagging
        runCatching { taggingService.tagItem(id) }
            .onFailure { log.warn("Tagging failed for {}: {}", id, it.message) }
    }
}