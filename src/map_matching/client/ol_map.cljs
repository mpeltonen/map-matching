(ns map-matching.client.ol-map
  (:require ["ol" :refer [Map, View]]
            ["ol/layer" :refer [Tile]]
            ["ol/source/OSM" :default OSM]
            ["ol/proj" :as proj]))

(def default-map-center [22.910 62.825])
(def default-zoom-level 13)

(defonce ol-view (atom nil))
(defonce ol-map (atom nil))

(defn- get-default-map-center []
  (proj/fromLonLat (clj->js default-map-center)))

(defn setup-map [target-id]
  (let [layers #js [
                    (Tile. #js {
                                :source (OSM.)})]
        view (View. #js {
                         :zoom default-zoom-level
                         :center (get-default-map-center)})
        map (Map. #js {
                       :target target-id
                       :layers layers
                       :view view})]
    (reset! ol-view view)
    (reset! ol-map map)))

(defn center-map []
  (let [size (.getSize @ol-map)
        extent #js [(/ (first size) 2), (/ (second size) 2)]]
    (.centerOn @ol-view (get-default-map-center) size extent)
    (.setZoom @ol-view default-zoom-level)))