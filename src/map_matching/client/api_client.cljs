(ns map-matching.client.api-client
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]))

(defn get-features []
  (http/get "/api/features"))
