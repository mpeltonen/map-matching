CREATE EXTENSION IF NOT EXISTS postgis;

create table feature
(
    id      serial primary key,
    uuid    char(36) unique,
    name    varchar unique,
    geom    geography(LINESTRING) unique
);