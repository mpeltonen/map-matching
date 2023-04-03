(ns map-matching.server.codec.teltonika
  (:require
    [clojure.java.io :as io]
    [map-matching.server.database :as db]
    [map-matching.server.websocket :as ws]
    [clojure.pprint :refer [pprint]])
  (:import [java.io DataInputStream DataOutputStream]
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
    (if (every? #(Character/isDigit %) imei)
      (do
        (println "Received offer for IMEI" (format "%s," imei) "accepting")
        (.writeByte out-stream 1)
        (.flush out-stream)
        imei)
      (throw (IllegalStateException. "Illegal IMEI")))))

; See https://wiki.teltonika-mobility.com/view/Full_AVL_ID_List
(def avl-props {
                240 "Movement"
                21 "GSM Signal"
                200 "Sleep mode"
                69 "GNSS Status"
                113 "Battery Level"
                181 "GNSS PDOP (x10)"
                182 "GNSS HDOP (x10)"
                24 "Speed"
                67 "Battery Voltage (x1000)"
                68 "Battery Current (x1000)"
                241 "Active GSM Operator"
                854 "User ID"})

(defn read-avl-properties [len count in-stream]
  (dotimes [_ count]
    (let [n-io-id (read-unsigned in-stream 2)
          n-value (read-unsigned in-stream len)]
      (print (str (get avl-props n-io-id (str "AVL ID " n-io-id)) "=" n-value ", ")))))

(defn read-io-element-8e [in-stream]
  (let [io-id (read-unsigned in-stream 2)
        n-total (read-unsigned in-stream 2)]
    (println "IO id:" io-id "N total:" n-total)
    (read-avl-properties 1 (read-unsigned in-stream 2) in-stream)
    (read-avl-properties 2 (read-unsigned in-stream 2) in-stream)
    (read-avl-properties 4 (read-unsigned in-stream 2) in-stream)
    (read-avl-properties 8 (read-unsigned in-stream 2) in-stream)
    (dotimes [_ (read-unsigned in-stream 2)]
      (let [nx-io-id (read-unsigned in-stream 2)
            nx-io-len (read-unsigned in-stream 2)
            _ (.skipBytes in-stream nx-io-len)]
        (println "Unsupported variable length AVL ID:" nx-io-id "length:" nx-io-len)))))

(defn handle-avl-record [imei codec-type in-stream]
  (let [ts (read-unsigned in-stream 8)
        priority (.readUnsignedByte in-stream)
        lon (double (/ (read-unsigned in-stream 4) 1e7))
        lat (double (/ (read-unsigned in-stream 4) 1e7))
        _ (.skipBytes in-stream 5) ; Skip altitude, angle & satellites
        speed (read-unsigned in-stream 2)]
    (println "\nTime:" (str (Date. ts)) "priority:" priority "latitude:" lat "longitude:" lon "speed:" speed)
    (db/insert-location ts imei lat lon)
    (cond
      (= 142 codec-type) (read-io-element-8e in-stream)
      :else (throw (RuntimeException. (str "Unsupported codec: " codec-type))))
    (ws/broadcast-msg (with-out-str (pprint {:imei imei :lat lat :lon lon})))))

(defn handle-avl-message [imei in-stream out-stream]
  (let [msg-length (read-unsigned in-stream 4)
        codec-type (.readUnsignedByte in-stream)
        num-records-1 (.readUnsignedByte in-stream)]
    (println "Received AVL message for codec" (format "0x%02x," codec-type) "length" msg-length "bytes," num-records-1 "data record(s)")
    (dotimes [_ num-records-1]
      (handle-avl-record imei codec-type in-stream))
    (let [num-records-2 (.readUnsignedByte in-stream)
          crc (read-unsigned in-stream 4)]
      (println "NR1==NR2:" (= num-records-1 num-records-2))
      (println "CRC:" crc)
      (.write out-stream (byte-array [0 0 0 num-records-1]) 0 4)
      (.flush out-stream))))

(defn handle-tcp-connection [socket]
  (let [in-stream (DataInputStream. (io/input-stream socket))
        out-stream (DataOutputStream. (io/output-stream socket))
        imei (atom nil)]
    (while (not (.isClosed socket))
      ;; See https://wiki.teltonika-gps.com/view/Teltonika_Data_Sending_Protocols,
      ;; "Communication with server" section(s).
      (let [preamble16 (.readUnsignedShort in-stream)]
        (if (> preamble16 0)
          (if (<= preamble16 15)
            (reset! imei (handle-imei-message preamble16 in-stream out-stream))
            (throw (IllegalStateException. "Illegal preamble")))
          (if @imei
            (do
              (.skipBytes in-stream 2)
              (handle-avl-message @imei in-stream out-stream))
            (throw (IllegalStateException. "No IMEI"))))))))
