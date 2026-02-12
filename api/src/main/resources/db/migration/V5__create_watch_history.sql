-- V5__create_watch_history.sql

create table if not exists watch_history (
    id          uuid primary key,
    media_id    uuid not null references media_items(id) on delete cascade,
    watched_at  date not null,
    rating      int null,
    status      text not null,
    notes       text null,
    created_at  timestamptz not null default now(),

    constraint chk_watch_history_rating
        check (rating is null or (rating >= 1 and rating <= 5)),

    constraint chk_watch_history_status
        check (status in (
               'WATCHED',
               'FINISHED',
               'READING',
               'WATCHING',
               'WANT_TO_WATCH',
               'WANT_TO_READ',
               'DROPPED'
            )
        )
);

create index if not exists idx_watch_history_media_id_watched_at
    on watch_history (media_id, watched_at desc);

create index if not exists idx_watch_history_status
    on watch_history (status);