-- V2__create_media_items.sql

create table if not exists media_items (
    id               uuid primary key,
    tmdb_id           text null,
    open_library_id   text null,
    title             text not null,
    media_type        text not null,
    year              int null,
    creator           text null,
    description       text null,
    poster_url        text null,
    created_at        timestamptz not null default now(),
    updated_at        timestamptz not null default now(),

    constraint chk_media_items_type
        check (media_type in ('BOOK', 'MOVIE')),

    -- enforce that both external IDs aren't set at once.
    constraint chk_media_items_external_id_by_type check (
        (media_type = 'MOVIE' and tmdb_id is not null and open_library_id is null) or
        (media_type = 'BOOK'  and open_library_id is not null and tmdb_id is null)
    )
);

-- Unique external IDs (partial unique indexes allow multiple NULLs)
create unique index if not exists uq_media_items_tmdb_id
    on media_items (tmdb_id)
    where tmdb_id is not null;

create unique index if not exists uq_media_items_open_library_id
    on media_items (open_library_id)
    where open_library_id is not null;

-- Common browse/sort patterns
create index if not exists idx_media_items_type_year
    on media_items (media_type, year);

create index if not exists idx_media_items_title
    on media_items (title);