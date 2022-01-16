(ns map-matching.client.ol-map
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require ["ol" :refer [Map, View, Feature, Overlay]]
            ["ol/layer/Tile" :default TileLayer]
            ["ol/layer/Vector" :default VectorLayer]
            ["ol/source/OSM" :default OSM]
            ["ol/source/Vector" :default VectorSource]
            ["ol/style" :refer [Style, Stroke, Circle, Fill]]
            ["ol/geom/Point" :default Point]
            ["ol/format/GeoJSON" :default GeoJSON]
            ["ol/proj" :as proj]
            [map-matching.client.api-client :as api-client]))

(def default-map-center [22.910 62.825])
(def default-zoom-level 13)

(defonce ol-view (atom nil))
(defonce ol-map (atom nil))
(defonce gps-trackers (atom {}))
(defonce gps-tracker-markers (atom {}))

(defn- add-marker [imei]
  (let [feature (Feature.)
        source (VectorSource. #js {
                                   :features (clj->js [feature])})
        stroke (Stroke. #js {
                             :color "blue"
                             :width "2"})
        fill (Fill. #js {
                         :color "red"})
        style (Style. #js {
                           :image (Circle. #js {
                                                :radius 4
                                                :stroke stroke
                                                :fill fill})})
        layer-config {:source source :style style}]
    (.addLayer @ol-map (VectorLayer. (clj->js layer-config)))
    (swap! gps-tracker-markers assoc-in [imei] feature)))

(defn- update-marker-position [imei lat lon]
  (let [marker-feature (get @gps-tracker-markers imei)
        pos (proj/fromLonLat (clj->js [lon lat]))
        point (Point. pos)]
    (.setGeometry marker-feature point)))

(defn set-tracker-location [{:keys [imei lat lon]}]
  (swap! gps-trackers assoc-in [imei] {:lat lat :lon lon})
  (when-not (get @gps-tracker-markers imei)
    (js/console.log "Adding new marker for IMEI" imei)
    (add-marker imei))
  (js/console.log "Updating marker position for IMEI" imei)
  (update-marker-position imei lat lon))

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


(def select-style (Style. #js {:fill (Fill. #js {:color "#eeeeee"})
                               :stroke (Stroke. #js {:color "rgba(255, 0, 0, 0.6)"
                                                     :width 6})}))
(def selected-feature (atom nil))

(defn set-selected-feature [feature overlay]
  (when @selected-feature
    (.setStyle @selected-feature js/undefined)
    (.setPosition overlay js/undefined))
  (reset! selected-feature feature))

(defn on-pointermove-or-click [evt]
  (let [pixel (.-pixel evt)
        coordinate (.-coordinate evt)
        overlay (.getOverlayById @ol-map 1)
        popup-content-elem (.getElement overlay)]
    (set-selected-feature nil overlay)
    (.forEachFeatureAtPixel @ol-map pixel
                            (fn [feature] (let [name (.get feature "name")]
                                            (.setStyle feature select-style)
                                            (.setPosition overlay coordinate)
                                            (set! (.-innerHTML popup-content-elem) (str "Name: " name))
                                            (set-selected-feature feature overlay)
                                            true)))))

(defn setup-map [target-id popup-elem]
  (let [layers #js [
                    (TileLayer. #js {
                                     :source (OSM.)})]
        view (View. #js {
                         :zoom default-zoom-level
                         :center (get-default-map-center)})
        overlay (Overlay. #js {:id 1
                               :element popup-elem})
        map (Map. #js {
                       :target target-id
                       :layers layers
                       :overlays #js [overlay]
                       :view view})]
    (reset! ol-view view)
    (reset! ol-map map)
    (get-features)
    (.on map "pointermove" on-pointermove-or-click)
    (.on map "click" on-pointermove-or-click)))


(defn center-map []
  (let [size (.getSize @ol-map)
        extent #js [(/ (first size) 2), (/ (second size) 2)]]
    (.centerOn @ol-view (get-default-map-center) size extent)
    (.setZoom @ol-view default-zoom-level)))