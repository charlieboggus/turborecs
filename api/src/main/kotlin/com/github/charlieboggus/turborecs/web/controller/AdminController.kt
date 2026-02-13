package com.github.charlieboggus.turborecs.web.controller

import com.github.charlieboggus.turborecs.service.AdminService
import com.github.charlieboggus.turborecs.web.dto.AdminStatsResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin")
class AdminController(
    private val adminService: AdminService
) {
    @GetMapping("/stats")
    fun stats(@RequestParam(required = false) modelVersion: String?): AdminStatsResponse =
        adminService.stats(modelVersion)
}