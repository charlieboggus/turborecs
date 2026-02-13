package com.github.charlieboggus.turborecs.web.controller

import com.github.charlieboggus.turborecs.config.properties.ClaudeProperties
import com.github.charlieboggus.turborecs.db.entity.enums.RecommendationSelection
import com.github.charlieboggus.turborecs.service.RecommendationService
import com.github.charlieboggus.turborecs.web.dto.RecommendationGridResponse
import com.github.charlieboggus.turborecs.web.dto.RecommendationTileResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/recommendations/grid")
class RecommendationGridController(
    private val recommendationService: RecommendationService,
    private val claudeProperties: ClaudeProperties
) {

    @GetMapping
    fun getGrid(
        @RequestParam selection: RecommendationSelection,
        @RequestParam(required = false) modelVersion: String?
    ): RecommendationGridResponse =
        recommendationService.getOrCreateGrid(selection, modelVersion ?: claudeProperties.model)

    @PostMapping
    fun newGrid(
        @RequestParam selection: RecommendationSelection,
        @RequestParam(required = false) modelVersion: String?
    ): RecommendationGridResponse =
        recommendationService.generateNewGrid(selection, modelVersion ?: claudeProperties.model)

    @PostMapping("/{batchId}/slots/{slot}/refresh")
    fun refreshSlot(
        @PathVariable batchId: UUID,
        @PathVariable slot: Int,
        @RequestParam selection: RecommendationSelection,
        @RequestParam(required = false) modelVersion: String?
    ): RecommendationTileResponse =
        recommendationService.refreshSlot(batchId, slot, selection, modelVersion ?: claudeProperties.model)
}