-- V6__db_generate_uuids.sql
-- Requires pgcrypto for gen_random_uuid()

create extension if not exists pgcrypto;

alter table media_items
    alter column id set default gen_random_uuid();

alter table tags
    alter column id set default gen_random_uuid();

alter table media_tags
    alter column id set default gen_random_uuid();

alter table watch_history
    alter column id set default gen_random_uuid();