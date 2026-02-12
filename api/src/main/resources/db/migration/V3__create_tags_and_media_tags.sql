-- V3__create_tags_and_media_tags.sql

create table if not exists tags (
    id         uuid primary key,
    name       text not null,
    category   text not null,

    constraint chk_tags_category
        check (category in ('THEME', 'MOOD', 'TONE', 'SETTING')),

    -- A tag name can exist in multiple categories (same string, different meaning)
    constraint uq_tags_category_name
        unique (category, name)
);

create table if not exists media_tags (
    id            uuid primary key,
    media_id      uuid not null references media_items(id) on delete cascade,
    tag_id        uuid not null references tags(id) on delete cascade,
    weight        double precision not null,
    generated_at  timestamptz not null default now(),
    model_version text not null,

    constraint chk_media_tags_weight
        check (weight >= 0.0 and weight <= 1.0),

    -- Prevent duplicates for a single model run/version
    constraint uq_media_tags_media_tag_model
        unique (media_id, tag_id, model_version)
);

create index if not exists idx_media_tags_media_id
    on media_tags (media_id);

create index if not exists idx_media_tags_tag_id
    on media_tags (tag_id);

create index if not exists idx_media_tags_generated_at
    on media_tags (generated_at);