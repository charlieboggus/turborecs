package com.github.charlieboggus.turborecs.controller

import com.github.charlieboggus.turborecs.common.enums.MediaSort
import com.github.charlieboggus.turborecs.common.enums.MediaType
import com.github.charlieboggus.turborecs.dto.request.LogMediaRequest
import com.github.charlieboggus.turborecs.dto.request.RateMediaRequest
import com.github.charlieboggus.turborecs.dto.response.MediaItemResponse
import com.github.charlieboggus.turborecs.service.MediaItemService
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/media")
class MediaItemController(
    private val mediaItemService: MediaItemService
) {
    @GetMapping
    fun getAll(
        @RequestParam(required = false) type: MediaType? = null,
        @RequestParam(required = false) sortBy: MediaSort? = null,
        @RequestParam(required = false) page: Int? = null,
        @RequestParam(required = false) limit: Int? = null
    ): Any {
        if (page != null && limit != null) {
            return mediaItemService.getAllItemsPaginated(type, page, limit, sortBy)
        }
        return mediaItemService.getAllItems(type, sortBy)
    }

    @GetMapping("/{id}")
    fun getById(
        @PathVariable id: UUID
    ): MediaItemResponse = mediaItemService.getItemById(id)

    @PostMapping
    fun logMediaItem(
        @RequestBody request: LogMediaRequest,
    ): MediaItemResponse = mediaItemService.logMediaItem(request)

    @PatchMapping("/{id}/rating")
    fun rateMediaItem(
        @PathVariable id: UUID,
        @RequestBody request: RateMediaRequest
    ): MediaItemResponse = mediaItemService.rateMediaItem(id, request.rating)

    @DeleteMapping("/{id}")
    fun deleteMediaItem(
        @PathVariable id: UUID,
    ) = mediaItemService.deleteMediaItem(id)
}