package com.github.charlieboggus.turborecs.controller

import com.github.charlieboggus.turborecs.dto.request.ExcludeMediaRequest
import com.github.charlieboggus.turborecs.dto.response.ExclusionResponse
import com.github.charlieboggus.turborecs.service.ExclusionService
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/exclusions")
class ExclusionController(
    private val exclusionService: ExclusionService
) {
    @GetMapping
    fun getAll(): List<ExclusionResponse> = exclusionService.getAllExclusions()

    @PostMapping
    fun excludeMediaItem(
        @RequestBody request: ExcludeMediaRequest
    ): ExclusionResponse = exclusionService.exclude(request)

    @DeleteMapping("/{id}")
    fun deleteExclusion(
        @PathVariable id: UUID
    ) = exclusionService.removeExclusion(id)
}