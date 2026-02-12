package com.github.charlieboggus.turborecs.db.repository

import com.github.charlieboggus.turborecs.db.entity.enums.TagCategory
import com.github.charlieboggus.turborecs.db.entity.TagEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TagRepository : JpaRepository<TagEntity, UUID> {
    fun findByCategoryAndName(category: TagCategory, name: String): TagEntity?
}