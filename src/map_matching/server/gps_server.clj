(ns map-matching.server.gps-server
  (:require
    [map-matching.server.codec.teltonika :refer [handle-tcp-connection]]
    [clojure.pprint :refer [pprint]])
  (:import [java.net InetAddress ServerSocket Socket SocketException]))

(defn remote-addr [socket]
  (str (.getRemoteSocketAddress socket)))

(defn finalize-connection [socket]
  (when-not (.isClosed socket)
    (.close socket))
  (println "Connection from" (remote-addr socket) "closed"))

(defn accept-connection [server-socket]
  (let [socket (.accept server-socket)]
    (println "New connection from" (remote-addr socket))
    (future
      (try (handle-tcp-connection socket)
           (catch Exception e
             (println (str "Error: " (.toString e))))
           (finally
             (finalize-connection socket))))))

(defn running? [server-socket]
  (not (.isClosed server-socket)))

(defn start-gps-server []
  (let [server-socket (ServerSocket. 7001 10)]
    (future
      (while (running? server-socket)
        (try
          (accept-connection server-socket)
          (catch SocketException e
            (println (str "Error: " (.toString e)))))))
    (println "Started TCP server on port 7001")))