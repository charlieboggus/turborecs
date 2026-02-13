package com.github.charlieboggus.turborecs.db.repository

import com.github.charlieboggus.turborecs.db.entity.TagEntity
import com.github.charlieboggus.turborecs.db.entity.enums.TagCategory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface TagRepository : JpaRepository<TagEntity, UUID> {

    @Query(
        """
        select t
        from TagEntity t
        where t.category = :category
          and lower(t.name) = lower(:name)
        """
    )
    fun findByCategoryAndNameIgnoreCase(
        @Param("category") category: TagCategory,
        @Param("name") name: String
    ): TagEntity?

    fun findAllByOrderByCategoryAscNameAsc(pageable: Pageable): Page<TagEntity>
}