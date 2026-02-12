-- V4__create_metadata_tables.sql

-- Movie-specific metadata
create table if not exists media_metadata (
    media_id         uuid primary key references media_items(id) on delete cascade,
    runtime_minutes  int null,
    genres           jsonb not null default '[]'::jsonb,

    constraint chk_media_metadata_runtime
        check (runtime_minutes is null or runtime_minutes > 0),

    -- genres is an array of strings in JSONB
    constraint chk_media_metadata_genres_is_array
        check (jsonb_typeof(genres) = 'array')
);

-- Book-specific metadata
create table if not exists book_metadata (
    media_id     uuid primary key references media_items(id) on delete cascade,
    page_count   int null,
    isbn         text null,
    publisher    text null,

    constraint chk_book_metadata_page_count
        check (page_count is null or page_count > 0)
);

create index if not exists idx_book_metadata_isbn
    on book_metadata (isbn)
    where isbn is not null;