(ns map-matching.server.main
  (:require [ring.adapter.jetty9 :as jetty]
            [map-matching.server.gps-server :refer [start-gps-server]]
            [map-matching.server.routing :refer [router]])
  (:gen-class))

(defn -main []
  (add-tap (bound-fn* clojure.pprint/pprint))
  (start-gps-server)
  (jetty/run-jetty #'router {:port 3001 :join? false}))