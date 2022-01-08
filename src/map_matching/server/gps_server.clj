(ns map-matching.server.gps-server
  (:require
    [clojure.java.io :as io]
    [map-matching.server.websocket :as ws]
    [clojure.pprint :refer [pprint]])
  (:import [java.net InetAddress ServerSocket Socket SocketException]
           [java.io DataInputStream DataOutputStream]
           [java.util Date]))

(defn read-unsigned [stream length]
  (let [bytes (byte-array length)]
    (.read stream bytes 0 length)
    (->> bytes
      (map (partial format "%02x"))
      (apply (partial str "0x"))
      (read-string))))

(defn handle-imei-message [imei-length in-stream out-stream]
  (let [imei-buf (byte-array imei-length)
        _ (.read in-stream imei-buf 0 imei-length)
        imei (String. imei-buf)]
    (println "Received offer for IMEI" (format "%s," imei) "accepting")
    (.writeByte out-stream 1)
    (.flush out-stream)
    imei))

(defn handle-avl-message [imei in-stream out-stream]
  (let [msg-length (read-unsigned in-stream 4)
        codec-type (.readUnsignedByte in-stream)
        num-records-1 (.readUnsignedByte in-stream)
        ts (read-unsigned in-stream 8)
        priority (.readUnsignedByte in-stream)
        lon (double (/ (read-unsigned in-stream 4) 1e7))
        lat (double (/ (read-unsigned in-stream 4) 1e7))
        _ (.skipBytes in-stream 5)
        speed (.readUnsignedShort in-stream)]
    (println "Received AVL message for codec" (format "0x%02x," codec-type) "length" msg-length "bytes," num-records-1 "data record(s)")
    (println "Time:" (str (Date. ts)) "priority:" priority "latitude:" lat "longitude:" lon "speed:" speed)
    (ws/broadcast-msg (with-out-str (pprint {:imei imei :lat lat :lon lon})))
    (.write out-stream (byte-array [0 0 0 num-records-1]) 0 4)
    (.flush out-stream)))

(defn handle-tcp-connection [socket]
  (let [in-stream (DataInputStream. (io/input-stream socket))
        out-stream (DataOutputStream. (io/output-stream socket))
        imei (atom nil)]
    (while (not (.isClosed socket))
      ;; See https://wiki.teltonika-gps.com/view/Teltonika_Data_Sending_Protocols,
      ;; "Communication with server" section(s).
      (let [preamble16 (.readUnsignedShort in-stream)]
        (if (> preamble16 0)
          (do
            (reset! imei (handle-imei-message preamble16 in-stream out-stream)))
          (do
            (.skipBytes in-stream 2)
            (handle-avl-message @imei in-stream out-stream)
            (.close socket)))))))

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
           (finally
             (finalize-connection socket))))))

(defn running? [server-socket]
  (not (.isClosed server-socket)))

(defn start-gps-server []
  (let [server-socket (ServerSocket. 7001 10 (InetAddress/getByName "127.0.0.1"))]
    (future
      (while (running? server-socket)
        (try
          (accept-connection server-socket)
          (catch SocketException e
            (println (str "caught exception: " (.toString e)))))))
    (println "Started TCP server on port 7001")))