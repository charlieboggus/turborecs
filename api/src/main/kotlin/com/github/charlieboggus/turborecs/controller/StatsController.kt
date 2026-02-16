package com.github.charlieboggus.turborecs.controller

import com.github.charlieboggus.turborecs.dto.response.StatsResponse
import com.github.charlieboggus.turborecs.service.StatsService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/stats")
class StatsController(
    private val statsService: StatsService
) {
    @GetMapping
    fun getStats(): StatsResponse = statsService.getStats()
}