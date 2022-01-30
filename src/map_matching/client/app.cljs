(ns map-matching.client.app
  (:require
    [reagent.core :as reagent]
    [reagent.dom :as d]
    [map-matching.client.pages :as pages]))

(def functional-compiler (reagent/create-compiler {:function-components true}))

(defn ^:dev/after-load mount-root []
      (let [root-el (.getElementById js/document "app")]
        (reagent/set-default-compiler! functional-compiler)
        (d/unmount-component-at-node root-el)
        (d/render [pages/index {:foo "foo"}] root-el)))