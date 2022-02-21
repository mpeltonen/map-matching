(ns map-matching.server.routing
  (:require
    [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
    [ring.middleware.reload :refer [wrap-reload]]
    [reitit.ring.middleware.parameters :refer [parameters-middleware]]
    [reitit.ring.middleware.exception :as exception]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [reitit.ring :as ring]
    [muuntaja.core :as m]
    [map-matching.server.handlers :as handlers]
    [map-matching.server.websocket :as ws]))

(def api-middlewares
  [muuntaja/format-middleware
   wrap-json-response
   parameters-middleware])

(def router
  (ring/ring-handler
    (ring/router
      [["/api" {:middleware api-middlewares}
        ["/features" {:get handlers/get-features
                      :post handlers/post-features}]]
       ["/websocket" {:get ws/handle-ws-request}]]
      {:data
       {:muuntaja m/instance
        :middleware [wrap-reload exception/exception-middleware]}})
    (ring/routes
      (ring/create-resource-handler {:path "/"})
      (ring/create-default-handler))))

