create table snapped_location
(
    tracker_location_id serial references tracker_location (id),
    feature_way_id      serial references feature (way_id),
    snapped_point       geography(POINT, 4326)
);
