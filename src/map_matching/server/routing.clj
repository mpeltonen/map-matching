(ns map-matching.server.routing
  (:require
    [ring.middleware.json :refer [wrap-json-response]]
    [ring.middleware.reload :refer [wrap-reload]]
    [reitit.ring.middleware.parameters :refer [parameters-middleware]]
    [reitit.ring :as ring]
    [map-matching.server.handlers :as handlers]))

(def api-middlewares
  [wrap-json-response
   parameters-middleware])

(def router
  (ring/ring-handler
    (ring/router
      ["/api" {:middleware api-middlewares}
       ["/time" {:get handlers/get-time}]]
      {:data
       {:middleware [wrap-reload]}})
    (ring/routes
      (ring/create-resource-handler {:path "/"})
      (ring/create-default-handler))))

