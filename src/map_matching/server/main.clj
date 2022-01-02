(ns map-matching.server.main
  (:require [ring.adapter.jetty :as jetty]
            [map-matching.server.gps-server :refer [start-gps-server]]
            [map-matching.server.routing :refer [router]])
  (:gen-class))

(defn -main []
  (start-gps-server)
  (jetty/run-jetty #'router {:port 3001 :join? false}))