(ns map-matching.server.map-matcher
  (:require [map-matching.server.database :as db]
            [clojure.data.xml :as xml])
  (:import (java.io File FileWriter)))

(defn- to-osm-xml [features]
  (let [node-map (atom {})
        gen-key (fn [node] (str (first node) (second node)))
        get-node (fn [node node-idx feature]
                   (let [node-map-key (gen-key node)
                         id-candidate (str (:way_id feature) node-idx)]
                     (if (contains? @node-map node-map-key)
                       nil
                       (get (swap! node-map assoc-in [node-map-key] {:id id-candidate
                                                                     :lon (first node)
                                                                     :lat (second node)}) node-map-key))))
        feature-node-to-osm-node (fn [feature] (fn [node-idx node] (get-node node node-idx feature)))
        feature-nodes-to-osm-nodes (fn [feature] (remove nil? (map-indexed (feature-node-to-osm-node feature) (:nodes feature))))
        feature-to-osm-way (fn [feature] {:id (:way_id feature)
                                          :nodes (mapv (fn [node]
                                                         {:ref (:id (get @node-map (gen-key node)))}) (:nodes feature))
                                          ; GraphHopper map matching engine requires these two OSM way tags for a way.
                                          :tags [{:k "name" :v (:name feature)} {:k "highway" :v "unclassified"}]})
        {nodes :nodes
         ways :ways} (reduce (fn [acc, feature] (let [feature-osm-nodes (doall (feature-nodes-to-osm-nodes feature))
                                                      feature-osm-way (feature-to-osm-way feature)]
                                                  (assoc acc
                                                    :nodes (concat (:nodes acc) feature-osm-nodes)
                                                    :ways (conj (:ways acc) feature-osm-way))))
                             {:nodes [] :ways []}
                             features)]
    (xml/element :osm {:version "0.6"}
                 (mapv #(xml/element :node %) nodes)
                 (mapv #(xml/element :way {:id (:id %)}
                                          (mapv (fn [n] (xml/element :nd {:ref (:ref n)})) (:nodes %))
                                          (mapv (fn [t] (xml/element :tag t)) (:tags %))) ways))))

(defn- create-osm-file []
  (let [osm-file (File/createTempFile "tmp" ".osm")]
    (with-open [osm-file-writer (FileWriter. osm-file)]
      (xml/emit (to-osm-xml (db/get-features)) osm-file-writer)
      (.deleteOnExit osm-file)
      (print "Exported features in OSM XML format to temporary file " (.getPath osm-file))
      osm-file)))

(defn initialize []
  (let [osm-file (create-osm-file)]
    nil))