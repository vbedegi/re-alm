(ns re-alm.io.websocket
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async :refer [chan <! >!]]
            [chord.client :refer [ws-ch]]
            [re-alm.core :as ra]))

(defn- make-ws-uri [path]
  (str "ws://" (.-host js/location) path))

(defn make-websocket! [url]
  (go
    (let [{:keys [ws-channel error]} (<! (ws-ch url #_{:format :json-kw}))]
      (if-not error
        ws-channel
        (js/console.log "Error:" (pr-str error))))))

(def store-ch (chan))

(go
  (loop [channels {}]
    (let [[path response-ch] (<! store-ch)]
      (if-let [ch (get channels path)]
        (do
          (>! response-ch ch)
          (recur channels))
        (let [ch (<! (make-websocket! (make-ws-uri path)))]
          (>! response-ch ch)
          (recur (assoc channels path ch)))))))

(defn- get-or-create-socket [path]
  (go
    (let [response-ch (chan)]
      (>! store-ch [path response-ch])
      (<! response-ch))))

(defrecord Websocket [path]
  ra/ITopic
  (make-event-source [this dispatch subscribers]
    (let [ch-ctrl (async/chan)]
      (go
        (let [ch-ws (<! (get-or-create-socket path))]
          (loop [subscribers subscribers]
            (let [[value ch] (async/alts! [ch-ctrl ch-ws])]
              (if (= ch ch-ctrl)
                (if (not= value :kill)
                  (recur (second value)))
                (do
                  (let [message (:message value)]
                    (ra/dispatch-to-subscribers dispatch subscribers message))
                  (recur subscribers)))))))
      ch-ctrl)))

(defn websocket [url msg]
  (ra/subscription (->Websocket url) msg))

(defrecord WebsocketFx [path message]
  ra/IEffect
  (execute [this dispatch]
    (go
      (let [ch-ws (<! (get-or-create-socket path))]
        (>! ch-ws message)))))

(defn websocket-fx [path message]
  (->WebsocketFx path message))
