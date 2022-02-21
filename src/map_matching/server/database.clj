(ns map-matching.server.database
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [next.jdbc.result-set :as rs]
            [next.jdbc.connection :as connection])
  (:import (com.zaxxer.hikari HikariDataSource)
           (org.flywaydb.core Flyway)))

(def db-url (System/getenv "DATABASE_URL"))

(defonce pool (connection/->pool HikariDataSource {:jdbcUrl db-url}))

(defn flyway-migrate []
  (-> (Flyway/configure)
      (.dataSource pool)
      (.baselineOnMigrate true)
      (.locations (into-array ["classpath:/migrations"]))
      (.failOnMissingLocations true)
      (.load)
      (.migrate)))

(def feature-collection-sql "select json_build_object(
       'type', 'FeatureCollection',
       'features', json_agg(features)
    ) as feature_collection
    from (select jsonb_build_object(
       'type', 'Feature',
       'id', uuid,
       'geometry', ST_AsGeoJSON(geom)::jsonb,
       'properties', to_jsonb(feature.*) - 'id' - 'uuid' - 'geom'
     ) as features
     from feature
     where geom is not null) as tmp")

(defn get-features []
  (with-open [conn (jdbc/get-connection pool)]
    (let [res (first (sql/query conn [feature-collection-sql]))]
      (if-not (empty? res)
        (str (:feature_collection res))
        "{}"))))

(def insert-feature-sql
  "insert into feature (uuid, name, geom)
   values (?, ?, ST_GeomFromText(?))
   on conflict (uuid) do update
   set name = excluded.name, geom = excluded.geom")

(defn- insert-feature [f tx]
  (let [coords (:coordinates (:geometry f))
        coords-wkt (clojure.string/join "," (map #(str (first %) " " (second %)) coords))
        geom-wkt (str "LINESTRING(" coords-wkt ")")]
    (jdbc/execute-one! tx [insert-feature-sql
                           (:id f)
                           (:name (:properties f))
                           geom-wkt])))

(defn save-featurecollection [fc]
  (let [features (:features fc)]
    (jdbc/with-transaction [tx pool]
      (doseq [f features]
        (insert-feature f tx)))))
