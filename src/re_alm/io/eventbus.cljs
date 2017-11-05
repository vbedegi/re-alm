(ns re-alm.io.eventbus
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async ]
            [re-alm.core :as ra]))

(def events-ch-by-topic (atom {}))

(defn- get-or-create-events-ch [topic]
  (get
    (swap! events-ch-by-topic (fn [chs]
                                (if (get chs topic)
                                  chs
                                  (assoc chs topic (async/chan)))))
    topic))

(defn- remove-events-ch [topic]
  (swap! events-ch-by-topic (fn [chs]
                              (when-let [ch (get chs topic)]
                                (async/close! ch)
                                (dissoc chs topic)))))

(defrecord Events [topic]
  ra/ITopic
  (make-event-source [this dispatch subscribers]
    (let [ch-ctrl (async/chan)
          ch-events (get-or-create-events-ch topic)]
      (go
        (loop [subscribers subscribers]
          (let [[v ch] (async/alts! [ch-ctrl ch-events])]
            (if (= ch ch-ctrl)
              (if (= v :kill)
                (remove-events-ch topic)
                (let [subscribers' (second v)]
                  (recur subscribers')))
              (do
                (ra/dispatch-to-subscribers dispatch subscribers v)
                (recur subscribers))))))
      ch-ctrl)))

(defn events [topic msg]
  (ra/subscription (->Events topic) msg))

(defrecord PublishFx [topic payload]
  ra/IEffect
  (execute [this dispatch]
    (when-let [events-ch (get @events-ch-by-topic topic)]
      (go
        (async/>! events-ch payload)))))

(defn publish-fx [topic payload]
  (->PublishFx topic payload))