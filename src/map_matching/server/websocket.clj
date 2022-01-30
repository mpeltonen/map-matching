(ns map-matching.server.websocket
  (:require
    [ring.util.response :refer [response]]
    [ring.adapter.jetty9 :as jetty]
    [clojure.pprint :as pp]
    [chime.core :refer [chime-at periodic-seq]])
  (:import [java.time Instant Duration]))

(defonce ws-clients (atom #{}))

(defn every-2-mins []
  (-> (periodic-seq (Instant/now) (Duration/ofMinutes 2)) rest))

(defn broadcast-msg [msg]
  (doseq [ws @ws-clients]
    (jetty/send! ws msg)))

(future
  (chime-at (every-2-mins)
    (fn [t]
      (broadcast-msg (with-out-str (pp/pprint {:ping (str t)}))))))

(defn- on-connect [ws]
  (tap> [:ws :connect (str (jetty/remote-addr ws))])
  (swap! ws-clients conj ws))

(defn- on-text [_ text-message]
  (tap> [:ws :msg text-message]))

(defn- on-close [ws status-code reason]
  (tap> [:ws :close status-code reason])
  (swap! ws-clients disj ws))

(defn- on-error [_ e]
  (tap> [:ws :error e]))

(defn- websocket-handler [upgrade-request]
  (let [subprotocols (:websocket-subprotocols upgrade-request)
        extensions (:websocket-extensions upgrade-request)]
    {
     :on-connect  on-connect
     :on-text     on-text
     :on-close    on-close
     :on-error    on-error
     :subprotocol (first subprotocols)
     :extentions  extensions}))

(defn handle-ws-request [req]
  (if (jetty/ws-upgrade-request? req)
    (jetty/ws-upgrade-response websocket-handler)
    {:status 200 :body "Plz upgrade!"}))

