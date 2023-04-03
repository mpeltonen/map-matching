CREATE EXTENSION IF NOT EXISTS postgis;

create table feature
(
    uuid    uuid primary key,
    way_id  serial unique not null,
    name    varchar(50) unique not null,
    geom    geography(LINESTRING) unique not null
);

create table tracker_location
(
    id        serial primary key,
    timestamp timestamp with time zone,
    imei      varchar(15) not null,
    location  geography(POINT, 4326)
);

create unique index imei_timestamp_idx on tracker_location (imei, timestamp desc);