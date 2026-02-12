-- V9__tags_name_citext.sql

create extension if not exists citext;

alter table tags
alter column name type citext;