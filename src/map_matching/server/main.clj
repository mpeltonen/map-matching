(ns map-matching.server.main
  (:require [ring.adapter.jetty9 :as jetty]
            [map-matching.server.gps-server :refer [start-gps-server]]
            [map-matching.server.routing :refer [router]]
            [map-matching.server.database :refer [flyway-migrate]]
            [map-matching.server.map-matcher :as mm])
  (:gen-class))

(defn -main []
  (add-tap (bound-fn* clojure.pprint/pprint))
  (start-gps-server)
  (flyway-migrate)
  (mm/initialize)
  (jetty/run-jetty #'router {:port 3001 :join? false}))