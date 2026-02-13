package com.github.charlieboggus.turborecs.web.controller

import com.github.charlieboggus.turborecs.config.properties.ClaudeProperties
import com.github.charlieboggus.turborecs.service.TaggingBatchService
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
@RequestMapping("/api/tagging")
class TaggingController(
    private val taggingService: TaggingService,
    private val taggingBatchService: TaggingBatchService,
    private val claudeProperties: ClaudeProperties
) {
    @PostMapping("/{mediaId}")
    fun tagOne(
        @PathVariable mediaId: UUID,
        @RequestParam(required = false) modelVersion: String?
    ) {
        taggingService.tagItem(mediaId, modelVersion ?: claudeProperties.model)
    }

    @PostMapping("/batch")
    fun tagBatch(
        @RequestParam(required = false, defaultValue = "200")
        @Min(1) @Max(1000)
        limit: Int,
        @RequestParam(required = false) modelVersion: String?
    ): List<UUID> {
        return taggingBatchService.tagAllUntagged(
            limit = limit,
            modelVersion = modelVersion ?: claudeProperties.model
        )
    }
}