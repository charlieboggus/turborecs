alter table media_metadata
    alter column genres type jsonb using genres::jsonb;

alter table media_metadata
    alter column genres set default '[]'::jsonb;

alter table media_metadata
    alter column genres set not null;