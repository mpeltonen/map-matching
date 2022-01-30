(ns map-matching.server.handlers
  (:require
    [map-matching.server.database :as db]
    [ring.util.response :refer [response header]]))

(defn json-response [body]
  (header (response body) "Content-Type" "application/json"))

(defn get-features [_]
  (json-response (db/get-features)))
