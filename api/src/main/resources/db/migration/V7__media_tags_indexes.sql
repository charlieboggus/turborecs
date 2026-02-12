-- V7__media_tags_indexes.sql

create index if not exists idx_media_tags_model_version_media_id
    on media_tags (model_version, media_id);