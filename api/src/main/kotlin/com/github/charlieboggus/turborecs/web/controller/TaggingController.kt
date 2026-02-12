package com.github.charlieboggus.turborecs.web.controller

import com.github.charlieboggus.turborecs.config.properties.ClaudeProperties
import com.github.charlieboggus.turborecs.service.TaggingService
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Validated
@RestController
@RequestMapping("/api/admin/tagging")
class TaggingController(
    private val taggingService: TaggingService,
    private val claudeProperties: ClaudeProperties
) {

    /**
     * Force tagging (or re-tagging) of a single item.
     * Useful after enrichment, prompt changes, or fixing broken tag rows.
     */
    @PostMapping("/{mediaId}")
    fun tagOne(
        @PathVariable mediaId: UUID,
        @RequestParam(required = false) modelVersion: String?
    ) {
        taggingService.tagItem(mediaId, modelVersion ?: claudeProperties.model)
    }

    /**
     * Tag media items that have NO tags for the given modelVersion.
     */
    @PostMapping("/batch")
    fun tagBatch(
        @RequestParam(required = false, defaultValue = "200")
        @Min(1) @Max(1000)
        limit: Int,
        @RequestParam(required = false) modelVersion: String?
    ): List<UUID> {
        return taggingService.tagAllUntagged(limit = limit, modelVersion = modelVersion ?: claudeProperties.model)
    }
}