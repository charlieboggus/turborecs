package com.github.charlieboggus.turborecs.web.controller

import com.github.charlieboggus.turborecs.service.EnrichmentService
import com.github.charlieboggus.turborecs.web.dto.MediaItemResponse
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/enrich")
class EnrichmentController(
    private val enrichmentService: EnrichmentService
) {
    @PostMapping("/{id}")
    fun enrichItem(@PathVariable id: UUID): MediaItemResponse =
        enrichmentService.enrichItem(id)

    @PostMapping("/batch")
    fun enrichAll(
        @RequestParam(required = false, defaultValue = "200") limit: Int
    ): List<MediaItemResponse> =
        enrichmentService.enrichAllUnenriched(limit)
}