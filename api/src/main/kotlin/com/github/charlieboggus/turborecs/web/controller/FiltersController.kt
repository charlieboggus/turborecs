package com.github.charlieboggus.turborecs.web.controller

import com.github.charlieboggus.turborecs.service.MediaFiltersService
import com.github.charlieboggus.turborecs.web.dto.MediaFiltersResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/media")
class FiltersController(
    private val mediaFiltersService: MediaFiltersService
) {
    @GetMapping("/filters")
    fun filters(@RequestParam(required = false) modelVersion: String?): MediaFiltersResponse =
        mediaFiltersService.getFilters(modelVersion)
}