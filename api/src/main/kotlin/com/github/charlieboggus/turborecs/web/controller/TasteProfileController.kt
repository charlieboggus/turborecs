package com.github.charlieboggus.turborecs.web.controller

import com.github.charlieboggus.turborecs.config.properties.ClaudeProperties
import com.github.charlieboggus.turborecs.service.TaggingService
import com.github.charlieboggus.turborecs.service.TasteProfile
import com.github.charlieboggus.turborecs.service.TasteProfileService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/taste-profile")
class TasteProfileController(
    private val tasteProfileService: TasteProfileService,
    private val claudeProperties: ClaudeProperties
) {
    @GetMapping
    fun getTasteProfile(
        @RequestParam(required = false) modelVersion: String?
    ): TasteProfile {
        return tasteProfileService.buildTasteProfile(modelVersion ?: claudeProperties.model)
    }
}