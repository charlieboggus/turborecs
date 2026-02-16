package com.github.charlieboggus.turborecs.controller

import com.github.charlieboggus.turborecs.common.enums.MediaType
import com.github.charlieboggus.turborecs.dto.response.RecommendationGridResponse
import com.github.charlieboggus.turborecs.service.RecommendationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/recommendations")
class RecommendationController(
    private val recommendationService: RecommendationService
) {
    @GetMapping
    fun getRecommendations(
        @RequestParam mediaType: MediaType? = null,
        @RequestParam generate: Boolean = true
    ): ResponseEntity<RecommendationGridResponse> {
        val grid = if (generate) {
            recommendationService.getOrRefreshGrid(mediaType)
        }
        else {
            recommendationService.getCachedGrid(mediaType)
        }
        return if (grid != null) {
            ResponseEntity.ok(grid)
        }
        else {
            ResponseEntity.noContent().build()
        }
    }

    @GetMapping("/refresh")
    fun forceRefresh(
        @RequestParam(required = false) mediaType: MediaType? = null,
    ): RecommendationGridResponse = recommendationService.forceRefreshGrid(mediaType)
}