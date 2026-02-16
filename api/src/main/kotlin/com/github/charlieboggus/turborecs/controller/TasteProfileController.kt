package com.github.charlieboggus.turborecs.controller

import com.github.charlieboggus.turborecs.dto.response.TasteProfileResponse
import com.github.charlieboggus.turborecs.service.TasteProfileService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/taste-profile")
class TasteProfileController(
    private val tasteProfileService: TasteProfileService
) {
    @GetMapping
    fun getTasteProfile(): TasteProfileResponse = tasteProfileService.buildTasteProfile()
}