-- =============================================================================
-- Turborecs v2 Schema
-- PostgreSQL 16+
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS citext;

-- ─────────────────────────────────────────────────────────────────────────────
-- MEDIA ITEMS
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE media_items (
                             id              uuid        PRIMARY KEY,
                             media_type      text        NOT NULL,
                             title           text        NOT NULL,
                             year            integer,
                             creator         text,
                             description     text,
                             poster_url      text,

    -- External IDs (populated during enrichment)
                             tmdb_id         text,
                             open_library_id text,

    -- User interaction
                             rating          integer,
                             consumed_at     date,

    -- Enrichment + tagging pipeline state
                             tagging_status  text        NOT NULL DEFAULT 'PENDING',

                             created_at      timestamptz NOT NULL DEFAULT now(),
                             updated_at      timestamptz NOT NULL DEFAULT now(),

                             CONSTRAINT chk_media_type
                                 CHECK (media_type IN ('MOVIE', 'BOOK')),
                             CONSTRAINT chk_rating
                                 CHECK (rating IS NULL OR (rating >= 1 AND rating <= 5)),
                             CONSTRAINT chk_tagging_status
                                 CHECK (tagging_status IN ('PENDING', 'TAGGED', 'FAILED')),
                             CONSTRAINT chk_external_id_by_type
                                 CHECK (
                                     (media_type = 'MOVIE' AND tmdb_id IS NOT NULL AND open_library_id IS NULL)
                                         OR
                                     (media_type = 'BOOK' AND open_library_id IS NOT NULL AND tmdb_id IS NULL)
                                     )
);

CREATE INDEX idx_media_items_type_year ON media_items (media_type, year);
CREATE INDEX idx_media_items_title ON media_items (title);
CREATE INDEX idx_media_items_tagging ON media_items (tagging_status) WHERE tagging_status = 'PENDING';
CREATE UNIQUE INDEX uq_media_items_tmdb_id ON media_items (tmdb_id) WHERE tmdb_id IS NOT NULL;
CREATE UNIQUE INDEX uq_media_items_open_library_id ON media_items (open_library_id) WHERE open_library_id IS NOT NULL;


-- ─────────────────────────────────────────────────────────────────────────────
-- MEDIA METADATA
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE media_metadata (
                                media_id        uuid        PRIMARY KEY REFERENCES media_items(id) ON DELETE CASCADE,
                                genres          jsonb       NOT NULL DEFAULT '[]',

    -- Movie-specific
                                runtime_minutes integer,

    -- Book-specific
                                page_count      integer,
                                isbn            text,
                                publisher       text,

                                CONSTRAINT chk_genres_is_array CHECK (jsonb_typeof(genres) = 'array'),
                                CONSTRAINT chk_runtime CHECK (runtime_minutes IS NULL OR runtime_minutes > 0),
                                CONSTRAINT chk_page_count CHECK (page_count IS NULL OR page_count > 0)
);

CREATE INDEX idx_media_metadata_isbn ON media_metadata (isbn) WHERE isbn IS NOT NULL;


-- ─────────────────────────────────────────────────────────────────────────────
-- TAGS
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE tags (
                      id          uuid    PRIMARY KEY,
                      name        citext  NOT NULL,
                      category    text    NOT NULL,

                      CONSTRAINT chk_tag_category CHECK (category IN ('THEME', 'MOOD', 'TONE', 'SETTING')),
                      CONSTRAINT uq_tags_category_name UNIQUE (category, name)
);


-- ─────────────────────────────────────────────────────────────────────────────
-- MEDIA TAGS
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE media_tags (
                            id              uuid            PRIMARY KEY,
                            media_id        uuid            NOT NULL REFERENCES media_items(id) ON DELETE CASCADE,
                            tag_id          uuid            NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
                            weight          double precision NOT NULL,
                            model_version   text            NOT NULL,
                            generated_at    timestamptz     NOT NULL DEFAULT now(),

                            CONSTRAINT chk_weight CHECK (weight >= 0.0 AND weight <= 1.0),
                            CONSTRAINT uq_media_tag_model UNIQUE (media_id, tag_id, model_version)
);

CREATE INDEX idx_media_tags_media_id ON media_tags (media_id);
CREATE INDEX idx_media_tags_tag_id ON media_tags (tag_id);
CREATE INDEX idx_media_tags_model_media ON media_tags (model_version, media_id);


-- ─────────────────────────────────────────────────────────────────────────────
-- EXCLUSIONS
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE exclusions (
                            id              uuid        PRIMARY KEY,
                            title           text        NOT NULL,
                            media_type      text        NOT NULL,
                            year            integer,
                            tmdb_id         text,
                            open_library_id text,
                            reason          text,
                            created_at      timestamptz NOT NULL DEFAULT now(),

                            CONSTRAINT chk_exclusion_type CHECK (media_type IN ('MOVIE', 'BOOK'))
);

CREATE INDEX idx_exclusions_tmdb ON exclusions (tmdb_id) WHERE tmdb_id IS NOT NULL;
CREATE INDEX idx_exclusions_openlibrary ON exclusions (open_library_id) WHERE open_library_id IS NOT NULL;


-- ─────────────────────────────────────────────────────────────────────────────
-- RECOMMENDATIONS LOG
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE recommendations_log (
                                     id              uuid        PRIMARY KEY,
                                     batch_id        uuid        NOT NULL,
                                     slot            integer     NOT NULL,
                                     model_version   text        NOT NULL,
                                     media_type      text        NOT NULL,
                                     title           text        NOT NULL,
                                     year            integer,
                                     creator         text,
                                     reason          text        NOT NULL,
                                     matched_tags    jsonb       NOT NULL DEFAULT '[]',
                                     fingerprint     text        NOT NULL,
                                     shown_at        timestamptz NOT NULL,
                                     expires_at      timestamptz NOT NULL,
                                     replaced_by     uuid,

                                     CONSTRAINT chk_reco_type CHECK (media_type IN ('MOVIE', 'BOOK')),
                                     CONSTRAINT uq_reco_batch_slot UNIQUE (batch_id, slot)
);

CREATE INDEX ix_reco_batch ON recommendations_log (batch_id);
CREATE INDEX ix_reco_active ON recommendations_log (model_version, expires_at);
CREATE INDEX ix_reco_fingerprint ON recommendations_log (model_version, fingerprint, expires_at);


-- ─────────────────────────────────────────────────────────────────────────────
-- HEALTHCHECK
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE turborecs_healthcheck (
                             id          bigint      PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                             status      text        NOT NULL DEFAULT 'OK',
                             created_at  timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_healthcheck_created_at ON turborecs_healthcheck (created_at);