package com.github.charlieboggus.turborecs.db.repository

import com.github.charlieboggus.turborecs.db.entity.BookMetadataEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface BookMetadataRepository : JpaRepository<BookMetadataEntity, UUID>