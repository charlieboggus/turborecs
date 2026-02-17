-- V3__add_dimension_vectors.sql

-- Per-item dimension vectors scored by Claude
CREATE TABLE media_vectors (
                               id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               media_item_id   UUID NOT NULL REFERENCES media_items(id) ON DELETE CASCADE,
                               model_version   VARCHAR(100) NOT NULL,

                               emotional_intensity   DOUBLE PRECISION NOT NULL DEFAULT 0.0,
                               narrative_complexity  DOUBLE PRECISION NOT NULL DEFAULT 0.0,
                               moral_ambiguity       DOUBLE PRECISION NOT NULL DEFAULT 0.0,
                               tone_darkness         DOUBLE PRECISION NOT NULL DEFAULT 0.0,
                               pacing                DOUBLE PRECISION NOT NULL DEFAULT 0.0,
                               humor                 DOUBLE PRECISION NOT NULL DEFAULT 0.0,
                               violence_intensity    DOUBLE PRECISION NOT NULL DEFAULT 0.0,
                               intellectual_depth    DOUBLE PRECISION NOT NULL DEFAULT 0.0,
                               stylistic_boldness    DOUBLE PRECISION NOT NULL DEFAULT 0.0,
                               intimacy_scale        DOUBLE PRECISION NOT NULL DEFAULT 0.0,
                               realism               DOUBLE PRECISION NOT NULL DEFAULT 0.0,
                               cultural_specificity  DOUBLE PRECISION NOT NULL DEFAULT 0.0,

                               created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

                               CONSTRAINT uq_media_vectors_item_model UNIQUE (media_item_id, model_version)
);

CREATE INDEX idx_media_vectors_model ON media_vectors(model_version);
CREATE INDEX idx_media_vectors_item  ON media_vectors(media_item_id);

-- Add matched_dimensions JSONB column to recommendation_log for explainability
ALTER TABLE recommendations_log
    ADD COLUMN matched_dimensions JSONB;