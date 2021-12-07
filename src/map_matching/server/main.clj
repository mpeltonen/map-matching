(ns map-matching.server.main
  (:require [ring.adapter.jetty :as jetty]
            [map-matching.server.routing :refer [router]])
  (:gen-class))

(defn -main []
  (jetty/run-jetty #'router {:port 3001 :join? false}))