(ns map-matching.client.websocket
  (:require [clojure.tools.reader :as reader]
            [map-matching.client.ol-map :refer [set-tracker-location]]))

(defonce ws (atom nil))

(defn construct-ws-url []
  (let [location (.-location js/window)
        http-proto (.-protocol location)
        ws-proto (clojure.string/replace http-proto #"http" "ws")
        host (.-host location)]
    (str ws-proto "//" host "/websocket")))

(defn js-console-pprint [data]
  (js/console.log (with-out-str (cljs.pprint/pprint data))))

(defn on-message [e]
  (let [data (reader/read-string (.-data e))]
    (js-console-pprint data)
    (when (:imei data)
      (set-tracker-location data))))

(defn on-close [e]
  (js/console.log "WebSocket closed, code:" (.-code e) "reason:" (str (.-reason e))))

(defn init []
  (reset! ws (js/WebSocket. (construct-ws-url)))
  (set! (.-onmessage @ws) on-message)
  (set! (.-onclose @ws) on-close))
