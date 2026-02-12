package com.github.charlieboggus.turborecs.db.repository

import com.github.charlieboggus.turborecs.db.entity.MediaMetadataEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MediaMetadataRepository : JpaRepository<MediaMetadataEntity, UUID>