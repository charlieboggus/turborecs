create table if not exists healthcheck (
    id          bigserial primary key,
    status      text not null default 'OK',
    created_at  timestamptz not null default now()
);

create index if not exists idx_healthcheck_created_at on healthcheck (created_at);