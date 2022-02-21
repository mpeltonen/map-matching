(ns map-matching.server.geojson
  (:require
    [map-matching.server.database :as db]))

(defn save-featurecollection [fc]
  (let [features (:features fc)]
    (doseq [f features]
      (db/insert-feature f))))
