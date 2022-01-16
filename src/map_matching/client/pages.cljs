(ns map-matching.client.pages
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require ["react" :as react]
            [map-matching.client.ol-map :as ol-map]))

(def map-element-id "map")

(defn index []
  (react/useEffect (fn [] (let [popup-elem (.getElementById js/document "popup")]
                            (ol-map/setup-map map-element-id popup-elem)) #js []))
  [:div
   [:div {:style {:padding "0.5rem"}}
    [:button {:on-click ol-map/center-map} "Reset view"]]
   [:div {:id map-element-id :class "map"}]
   [:div {:id "popup" :class "ol-popup"}
    [:div ""]]])