-- V11__create_recommendations_log.sql

create table if not exists recommendations_log (
    id uuid primary key default gen_random_uuid(),

    model_version text not null,
    selection text not null, -- 'BOOKS' | 'MOVIES' | 'BOTH'

    media_type text not null, -- 'BOOK' | 'MOVIE'
    title text not null,
    year int null,
    creator text null,

    reason text not null,
    matched_themes jsonb not null default '[]'::jsonb,

    fingerprint text not null,

    batch_id uuid not null,
    slot int not null,

    shown_at timestamptz not null,
    expires_at timestamptz not null,

    replaced_by uuid null,

    constraint uq_reco_batch_slot unique (batch_id, slot)
);

create index if not exists ix_reco_active
    on recommendations_log (model_version, expires_at);

create index if not exists ix_reco_fingerprint_active
    on recommendations_log (model_version, fingerprint, expires_at);

create index if not exists ix_reco_batch
    on recommendations_log (batch_id);