package com.github.charlieboggus.turborecs.db.entity

import com.github.charlieboggus.turborecs.db.entity.enums.TagCategory
import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(
    name = "tags",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_tags_category_name",
            columnNames = ["category", "name"]
        )
    ]
)
class TagEntity(
    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "uuid")
    var id: UUID? = null,

    @Column(name = "name", nullable = false)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    var category: TagCategory
)