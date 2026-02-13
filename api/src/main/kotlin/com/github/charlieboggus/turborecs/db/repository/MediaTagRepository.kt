package com.github.charlieboggus.turborecs.db.repository

import com.github.charlieboggus.turborecs.db.entity.MediaTagEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface MediaTagRepository : JpaRepository<MediaTagEntity, UUID> {

    @Modifying
    @Query(
        """
    delete from MediaTagEntity mt
    where mt.media.id = :mediaId
      and mt.modelVersion = :modelVersion
    """
    )
    fun deleteByMediaIdAndModelVersion(
        @Param("mediaId") mediaId: UUID,
        @Param("modelVersion") modelVersion: String
    ): Int

    @Query(
        value = """
        select mi.id
        from media_items mi
        left join media_tags mt
          on mt.media_id = mi.id
         and mt.model_version = :modelVersion
        where mt.id is null
        order by mi.created_at desc
        limit :limit
        """,
        nativeQuery = true
    )
    fun findUntaggedMediaIds(
        @Param("modelVersion") modelVersion: String,
        @Param("limit") limit: Int
    ): List<UUID>

    interface TasteRow {
        fun getMediaId(): UUID
        fun getTitle(): String
        fun getRating(): Int
        fun getCategory(): String
        fun getTagName(): String
        fun getTagWeight(): Double
    }

    /**
     * Returns one row per (rated media item, tag). Uses the *latest* rated watch_history
     * entry per media_id (rating not null), then joins tags for the given model_version.
     */
    @Query(
        value = """
    with latest_rated as (
        select distinct on (wh.media_id)
               wh.media_id,
               wh.rating,
               wh.created_at
        from watch_history wh
        where wh.rating is not null
        order by wh.media_id, wh.created_at desc
    )
    select
        lr.media_id              as mediaId,
        mi.title                 as title,
        lr.rating                as rating,
        (t.category)::text       as category,
        (t.name)::text           as tagName,
        mt.weight                as tagWeight
    from latest_rated lr
    join media_items mi
      on mi.id = lr.media_id
    join media_tags mt
      on mt.media_id = lr.media_id
     and mt.model_version = :modelVersion
    join tags t
      on t.id = mt.tag_id
    """,
        nativeQuery = true
    )
    fun fetchTasteRows(@Param("modelVersion") modelVersion: String): List<TasteRow>

    interface TagRow {
        fun getCategory(): String
        fun getName(): String
        fun getWeight(): Double
    }

    @Query(
        value = """
    select
      (t.category)::text as category,
      (t.name)::text     as name,
      mt.weight          as weight
    from media_tags mt
    join tags t on t.id = mt.tag_id
    where mt.media_id = :id
      and mt.model_version = :modelVersion
    order by t.category, mt.weight desc
    """,
        nativeQuery = true
    )
    fun fetchTagsForMedia(
        @Param("id") id: UUID,
        @Param("modelVersion") modelVersion: String
    ): List<TagRow>

    interface PopularTagRow {
        fun getCategory(): String
        fun getName(): String
        fun getCnt(): Long
    }

    @Query(
        value = """
        select
            (t.category)::text as category,
            (t.name)::text     as name,
            count(*)           as cnt
        from media_tags mt
        join tags t on t.id = mt.tag_id
        where (:modelVersion is null or mt.model_version = :modelVersion)
        group by t.category, t.name
        order by cnt desc
        limit :limit
        """,
        nativeQuery = true
    )
    fun findPopularTags(
        @Param("modelVersion") modelVersion: String?,
        @Param("limit") limit: Int
    ): List<PopularTagRow>

    @Query(
        value = """
        select count(distinct mt.media_id)
        from media_tags mt
        where mt.model_version = :modelVersion
        """,
        nativeQuery = true
    )
    fun countDistinctTaggedMedia(@Param("modelVersion") modelVersion: String): Long
}