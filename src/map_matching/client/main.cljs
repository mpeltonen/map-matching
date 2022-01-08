(ns map-matching.client.main
  (:require [map-matching.client.app :as app]
            [map-matching.client.websocket :as ws]))

(defn main []
      (app/mount-root)
      (ws/init))