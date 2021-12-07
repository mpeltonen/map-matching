(ns map-matching.client.app
  (:require
    [reagent.dom :as d]
    [re-frame.core :as r]
    [map-matching.client.pages :as pages]))

(defn ^:dev/after-load mount-root []
      (r/clear-subscription-cache!)
      (let [root-el (.getElementById js/document "app")]
           (d/unmount-component-at-node root-el)
           (d/render [pages/index] root-el)))