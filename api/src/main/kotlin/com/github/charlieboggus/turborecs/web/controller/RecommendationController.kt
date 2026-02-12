package com.github.charlieboggus.turborecs.web.controller

import com.github.charlieboggus.turborecs.config.properties.ClaudeProperties
import com.github.charlieboggus.turborecs.db.entity.enums.MediaType
import com.github.charlieboggus.turborecs.service.Recommendation
import com.github.charlieboggus.turborecs.service.RecommendationService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/recommendations")
class RecommendationController(
    private val recommendationService: RecommendationService,
    private val claudeProperties: ClaudeProperties
) {

    @GetMapping
    fun recommend(
        @RequestParam(required = false, defaultValue = "10") count: Int,
        @RequestParam(required = false) mediaType: MediaType?,
        @RequestParam(required = false) modelVersion: String?,
    ): List<Recommendation> {
        val mv = modelVersion?.trim().takeUnless { it.isNullOrBlank() } ?: claudeProperties.model
        return recommendationService.recommend(
            count = count,
            mediaType = mediaType,
            modelVersion = mv
        )
    }
}