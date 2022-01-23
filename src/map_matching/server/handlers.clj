(ns map-matching.server.handlers
  (:require
    [map-matching.server.database :as db]
    [ring.util.response :refer [response header]]))

(defn json-response [body]
  (header (response body) "Content-Type" "application/json"))

(defn get-time [req]
  (clojure.pprint/pprint {:headers (:headers req) :query-params (:query-params req)})
  (response {:time (.toString (java.util.Date.))}))

(defn get-features [req]
  (json-response (db/get-features)))
