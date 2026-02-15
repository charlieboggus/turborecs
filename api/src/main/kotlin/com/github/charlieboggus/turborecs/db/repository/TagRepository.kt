package com.github.charlieboggus.turborecs.db.repository

import com.github.charlieboggus.turborecs.common.enums.TagCategory
import com.github.charlieboggus.turborecs.db.entities.TagEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TagRepository : JpaRepository<TagEntity, UUID> {

    // Find-or-create pattern: check if a tag already exists before inserting
    fun findByCategoryAndName(category: TagCategory, name: String): TagEntity?

    // Taste profile: get all tags in a category
    fun findAllByCategory(category: TagCategory): List<TagEntity>
}