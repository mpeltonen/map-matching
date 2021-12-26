(ns map-matching.client.ol-map
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require ["ol" :refer [Map, View]]
            ["ol/layer/Tile" :default TileLayer]
            ["ol/layer/Vector" :default VectorLayer]
            ["ol/source/OSM" :default OSM]
            ["ol/source/Vector" :default VectorSource]
            ["ol/style/Style" :default Style]
            ["ol/style/Stroke" :default Stroke]
            ["ol/format/GeoJSON" :default GeoJSON]
            ["ol/proj" :as proj]
            [map-matching.client.api-client :as api-client]))

(def default-map-center [22.910 62.825])
(def default-zoom-level 13)

(defonce ol-view (atom nil))
(defonce ol-map (atom nil))

(defn- get-default-map-center []
  (proj/fromLonLat (clj->js default-map-center)))

(defn- render-features [geo-json]
  (let [featureProjection #js {:featureProjection "EPSG:3857"}
        features (.readFeatures (GeoJSON.) (clj->js geo-json) featureProjection)
        vector-source (VectorSource. #js {:features features})
        layer-config {:source vector-source
                      :style (Style. #js {:stroke (Stroke. #js {:color "#FF0000" :width "3"})})}]
    (.addLayer @ol-map (VectorLayer. (clj->js layer-config)))))

(defn- get-features []
  (go (let [resp (<! (api-client/get-features))
            geo-json (:body resp)]
        (render-features geo-json))))

(defn setup-map [target-id]
  (let [layers #js [
                    (TileLayer. #js {
                                     :source (OSM.)})]
        view (View. #js {
                         :zoom default-zoom-level
                         :center (get-default-map-center)})
        map (Map. #js {
                       :target target-id
                       :layers layers
                       :view view})]
    (reset! ol-view view)
    (reset! ol-map map)
    (get-features)))

(defn center-map []
  (let [size (.getSize @ol-map)
        extent #js [(/ (first size) 2), (/ (second size) 2)]]
    (.centerOn @ol-view (get-default-map-center) size extent)
    (.setZoom @ol-view default-zoom-level)))