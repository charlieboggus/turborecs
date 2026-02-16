package com.github.charlieboggus.turborecs.controller

import com.github.charlieboggus.turborecs.common.enums.MediaType
import com.github.charlieboggus.turborecs.dto.response.SearchResultResponse
import com.github.charlieboggus.turborecs.service.SearchService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/search")
class SearchController(
    private val searchService: SearchService
) {
    @GetMapping
    fun search(
        @RequestParam(required = true) query: String,
        @RequestParam(required = true) type: MediaType
    ): List<SearchResultResponse> = searchService.search(query, type)
}