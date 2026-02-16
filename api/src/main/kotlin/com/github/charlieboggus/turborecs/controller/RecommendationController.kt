package com.github.charlieboggus.turborecs.controller

import com.github.charlieboggus.turborecs.common.enums.MediaType
import com.github.charlieboggus.turborecs.dto.response.RecommendationGridResponse
import com.github.charlieboggus.turborecs.service.RecommendationService
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
    fun getGrid(
        @RequestParam(required = false) mediaType: MediaType? = null,
    ): RecommendationGridResponse = recommendationService.getOrRefreshGrid(mediaType)

    @GetMapping("/refresh")
    fun forceRefresh(
        @RequestParam(required = false) mediaType: MediaType? = null,
    ): RecommendationGridResponse = recommendationService.forceRefreshGrid(mediaType)
}