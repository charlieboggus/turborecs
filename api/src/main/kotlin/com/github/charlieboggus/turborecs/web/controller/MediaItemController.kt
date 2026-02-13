package com.github.charlieboggus.turborecs.web.controller

import com.github.charlieboggus.turborecs.db.entity.enums.MediaStatus
import com.github.charlieboggus.turborecs.db.entity.enums.MediaType
import com.github.charlieboggus.turborecs.service.MediaItemService
import com.github.charlieboggus.turborecs.service.enums.MediaSort
import com.github.charlieboggus.turborecs.web.dto.BulkNotesRequest
import com.github.charlieboggus.turborecs.web.dto.BulkRatingRequest
import com.github.charlieboggus.turborecs.web.dto.BulkStatusRequest
import com.github.charlieboggus.turborecs.web.dto.CreateBookRequest
import com.github.charlieboggus.turborecs.web.dto.CreateMovieRequest
import com.github.charlieboggus.turborecs.web.dto.MediaItemDetailResponse
import com.github.charlieboggus.turborecs.web.dto.MediaItemResponse
import com.github.charlieboggus.turborecs.web.dto.PageResponse
import com.github.charlieboggus.turborecs.web.dto.RateItemRequest
import com.github.charlieboggus.turborecs.web.dto.UpdateMediaItemRequest
import com.github.charlieboggus.turborecs.web.dto.UpdateNotesRequest
import com.github.charlieboggus.turborecs.web.dto.UpdateStatusRequest
import com.github.charlieboggus.turborecs.web.dto.WatchHistoryDto
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/media")
class MediaItemController(
    private val service: MediaItemService
) {

    @GetMapping
    fun getAll(
        @RequestParam type: MediaType? = null,
        @RequestParam status: MediaStatus? = null,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int
    ): PageResponse<MediaItemResponse> =
        service.getAllItemsPaged(type = type, status = status, page = page, size = size)

    @GetMapping("/{id}")
    fun getById(
        @PathVariable id: UUID,
        @RequestParam(required = false) modelVersion: String?
    ): MediaItemDetailResponse =
        service.getItemDetailById(id, modelVersion)

    @GetMapping("/search/all")
    fun search(@RequestParam query: String): List<MediaItemResponse> =
        service.searchByTitle(query)

    @PostMapping("/movies")
    @ResponseStatus(HttpStatus.CREATED)
    fun createMovie(@Valid @RequestBody request: CreateMovieRequest): MediaItemResponse =
        service.createMovie(request)

    @PostMapping("/books")
    @ResponseStatus(HttpStatus.CREATED)
    fun createBook(@Valid @RequestBody request: CreateBookRequest): MediaItemResponse =
        service.createBook(request)

    @PatchMapping("/{id}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun updateStatus(@PathVariable id: UUID, @Valid @RequestBody request: UpdateStatusRequest) {
        service.updateStatus(id, request.status)
    }

    @PatchMapping("/{id}/rating")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun rateItem(@PathVariable id: UUID, @Valid @RequestBody request: RateItemRequest) {
        service.rateItem(id, request.rating)
    }

    @PatchMapping("/{id}/notes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun updateNotes(@PathVariable id: UUID, @Valid @RequestBody request: UpdateNotesRequest) {
        service.updateNotes(id, request.notes)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: UUID) {
        service.deleteItem(id)
    }

    @GetMapping("/search")
    fun searchPaged(
        @RequestParam query: String,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "50") size: Int
    ): PageResponse<MediaItemResponse> =
        service.searchByTitlePaged(query, page, size)

    @GetMapping("/{id}/history")
    fun history(@PathVariable id: UUID): List<WatchHistoryDto> =
        service.getHistory(id)

    @PatchMapping("/{id}")
    fun updateCoreFields(
        @PathVariable id: UUID,
        @Valid @RequestBody req: UpdateMediaItemRequest
    ): MediaItemResponse =
        service.updateMediaItemCoreFields(id, req)

    @PatchMapping("/bulk/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun bulkStatus(@Valid @RequestBody req: BulkStatusRequest) {
        service.bulkUpdateStatus(req)
    }

    @PatchMapping("/bulk/rating")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun bulkRating(@Valid @RequestBody req: BulkRatingRequest) {
        service.bulkUpdateRating(req)
    }

    @PatchMapping("/bulk/notes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun bulkNotes(@Valid @RequestBody req: BulkNotesRequest) {
        service.bulkUpdateNotes(req)
    }
}