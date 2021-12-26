(ns map-matching.client.pages
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as reagent]
            ["react" :as react]
            [map-matching.client.ol-map :as ol-map]
            [cljs.core.async :refer [<!]]
            [map-matching.client.api-client :as api-client]))

(defonce tm (reagent/atom ""))

(defn get-time []
  (go (let [resp (<! (api-client/get-time))]
        (reset! tm (:time (:body resp))))))

(defn index []
  (let [[count set-count] (react/useState 0)]
    (react/useEffect #(ol-map/setup-map) #js [])
    [:div
     [:p (str "Count: " count)]
     [:p (str "Time: " @tm)]
     [:button {:on-click #(set-count (inc count))} "Inc count"]
     [:button {:on-click get-time} "Get time"]
     [:button {:on-click ol-map/center-map } "Reset view"]
     [:div#map {:style {:height "100vh" :width "100vw"}}]]))