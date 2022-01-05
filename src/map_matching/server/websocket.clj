(ns map-matching.server.websocket
  (:require
    [ring.util.response :refer [response]]
    [ring.adapter.jetty9 :as jetty]))

(defonce ws-clients (atom #{}))

(defn- on-connect [ws]
  (tap> [:ws :connect (str (jetty/remote-addr ws))])
  (swap! ws-clients conj ws))

(defn- on-text [ws text-message]
  (tap> [:ws :msg text-message])
  (jetty/send! ws (str "echo: " text-message)))

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

