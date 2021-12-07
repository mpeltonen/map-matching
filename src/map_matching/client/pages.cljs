(ns map-matching.client.pages
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as reagent]
            [cljs.core.async :refer [<!]]
            [map-matching.client.api-client :as api-client]))

(defonce tm (reagent/atom ""))

(defn get-time []
  (go (let [resp (<! (api-client/get-time))]
        (reset! tm (:time (:body resp))))))

(defn index []
  [:div
   [:p @tm]
   [:button {:on-click get-time} "Get time"]])