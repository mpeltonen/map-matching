(ns map-matching.server.handlers
  (:require
    [map-matching.server.database :as db]
    [ring.util.response :refer [response header status]]))

(defn resp [body status-code content-type]
  (-> body
      (response)
      (status status-code)
      (header "Content-Type" content-type)))

(defn ok-json-response [body]
  (resp body 200 "application/json"))

(defn error-text-response [body]
  (resp body 500 "text/plain"))

(defn get-features [_]
  (ok-json-response (db/get-features)))

(defn post-features [req]
  (let [body (:body-params req)]
    (try
      (db/save-featurecollection body)
      (ok-json-response [])
      (catch Exception e
        (do
          (println (str (.toString e)))
          (error-text-response (.getMessage e)))))))
