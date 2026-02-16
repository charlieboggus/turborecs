package com.github.charlieboggus.turborecs.db.entities

import com.github.charlieboggus.turborecs.common.enums.TagCategory

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "tags")
class TagEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "name", nullable = false, columnDefinition = "citext")
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    val category: TagCategory
)