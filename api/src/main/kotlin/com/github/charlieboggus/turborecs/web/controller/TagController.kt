package com.github.charlieboggus.turborecs.web.controller

import com.github.charlieboggus.turborecs.db.repository.MediaTagRepository
import com.github.charlieboggus.turborecs.db.repository.TagRepository
import com.github.charlieboggus.turborecs.web.dto.TagDto
import com.github.charlieboggus.turborecs.web.dto.TagSummaryDto
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/tags")
class TagController(
    private val tagRepository: TagRepository,
    private val mediaTagRepository: MediaTagRepository
) {
    @GetMapping
    fun list(
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "100") size: Int
    ): List<TagDto> {
        val p = tagRepository.findAllByOrderByCategoryAscNameAsc(PageRequest.of(page, size))
        return p.content.map {
            TagDto(
                id = requireNotNull(it.id),
                category = it.category.name,
                name = it.name
            )
        }
    }

    @GetMapping("/popular")
    fun popular(
        @RequestParam(required = false) modelVersion: String?,
        @RequestParam(required = false, defaultValue = "50") limit: Int
    ): List<TagSummaryDto> {
        val safeLimit = limit.coerceIn(1, 200)
        return mediaTagRepository.findPopularTags(modelVersion, safeLimit)
            .map { TagSummaryDto(category = it.getCategory(), name = it.getName(), count = it.getCnt()) }
    }
}