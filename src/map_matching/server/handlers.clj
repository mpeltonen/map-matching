(ns map-matching.server.handlers
  (:require
    [ring.util.response :refer [response]]))

(defn get-time [req]
  (clojure.pprint/pprint {:headers (:headers req) :query-params (:query-params req)})
  (response {:time (.toString (java.util.Date.))}))