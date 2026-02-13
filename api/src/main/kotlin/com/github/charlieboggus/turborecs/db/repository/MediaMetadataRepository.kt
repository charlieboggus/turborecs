package com.github.charlieboggus.turborecs.db.repository

import com.github.charlieboggus.turborecs.db.entity.MediaMetadataEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface MediaMetadataRepository : JpaRepository<MediaMetadataEntity, UUID> {

    @Query(
        value = """
        select distinct g.genre
        from media_metadata mm
        cross join lateral jsonb_array_elements_text(mm.genres) as g(genre)
        where g.genre is not null and g.genre <> ''
        order by g.genre asc
        """,
        nativeQuery = true
    )
    fun findDistinctGenres(): List<String>
}