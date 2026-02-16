package com.github.charlieboggus.turborecs.service

import com.github.charlieboggus.turborecs.common.enums.MediaType
import com.github.charlieboggus.turborecs.db.entities.ExclusionEntity
import com.github.charlieboggus.turborecs.db.repository.ExclusionRepository
import com.github.charlieboggus.turborecs.dto.request.ExcludeMediaRequest
import com.github.charlieboggus.turborecs.dto.response.ExclusionResponse
import com.github.charlieboggus.turborecs.error.ItemAlreadyExistsException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ExclusionService(
    private val exclusionRepository: ExclusionRepository
) {
    @Transactional(readOnly = true)
    fun getAllExclusions(): List<ExclusionResponse> {
        return exclusionRepository.findAll().map { ExclusionResponse.from(it) }
    }

    @Transactional
    fun exclude(request: ExcludeMediaRequest): ExclusionResponse {
        if (request.mediaType == MediaType.MOVIE && request.tmdbId != null) {
            if (exclusionRepository.existsByTmdbId(request.tmdbId)) {
                throw ItemAlreadyExistsException("Movie already excluded")
            }
        }
        if (request.mediaType == MediaType.BOOK && request.openLibraryId != null) {
            if (exclusionRepository.existsByOpenLibraryId(request.openLibraryId)) {
                throw ItemAlreadyExistsException("Book already excluded")
            }
        }
        val entity = ExclusionEntity(
            title = request.title,
            mediaType = request.mediaType,
            year = request.year,
            tmdbId = request.tmdbId,
            openLibraryId = request.openLibraryId,
            reason = request.reason
        )
        val saved = exclusionRepository.save(entity)
        return ExclusionResponse.from(saved)
    }

    @Transactional
    fun removeExclusion(id: UUID) {
        if (!exclusionRepository.existsById(id)) {
            throw NoSuchElementException("Exclusion not found")
        }
        exclusionRepository.deleteById(id)
    }
}