(ns map-matching.client.pages
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require ["react" :as react]
            [map-matching.client.ol-map :as ol-map]
            [cljs.core.async :refer [<!]]
            [map-matching.client.api-client :as api-client]))

(def map-element-id "map")

(defn index []
  (react/useEffect #(ol-map/setup-map map-element-id) #js [])
  [:div
    [:button {:on-click ol-map/center-map } "Reset view"]
    [:div {:id map-element-id :style {:height "100vh" :width "100vw"}}]])