CREATE EXTENSION IF NOT EXISTS postgis;

create table feature
(
    uuid    uuid primary key,
    way_id  serial unique not null,
    name    varchar(50) unique not null,
    geom    geography(LINESTRING) unique not null
);
