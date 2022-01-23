(ns map-matching.server.database
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [next.jdbc.result-set :as rs]
            [next.jdbc.connection :as connection])
  (:import (com.zaxxer.hikari HikariDataSource)
           (org.flywaydb.core Flyway)))

(def db-url (System/getenv "DATABASE_URL"))

(def pool (connection/->pool HikariDataSource {:jdbcUrl db-url}))

(defn flyway-migrate []
  (-> (Flyway/configure)
      (.dataSource pool)
      (.baselineOnMigrate true)
      (.locations (into-array ["classpath:/migrations"]))
      (.failOnMissingLocations true)
      (.load)
      (.migrate)))

(defn get-features []
  (with-open [conn (jdbc/get-connection pool)]
    (let [res (first (sql/query conn ["select geojson from feature limit 1"]))]
      (if-not (empty? res)
        (str (:feature/geojson res))
        "{}"))))
