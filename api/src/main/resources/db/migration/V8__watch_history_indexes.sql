-- V8__watch_history_indexes.sql

create index if not exists idx_watch_history_rated_media_created
    on watch_history (media_id, created_at desc)
    where rating is not null;

create index if not exists idx_watch_history_media_created
    on watch_history (media_id, created_at desc);
