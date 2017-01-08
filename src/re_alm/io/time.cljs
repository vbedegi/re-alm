(ns re-alm.io.time
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [re-alm.core :as ra]
            [cljs.core.async :as async]))

(defrecord Time [interval]
  ra/ITopic
  (make-event-source [this dispatch subscribers]
    (let [ch-ctrl (async/chan)]
      (go
        (loop [subscribers subscribers]
          (let [[value ch] (async/alts! [ch-ctrl (async/timeout interval)])]
            (if (= ch ch-ctrl)
              (if (not= value :kill)
                (recur (second value)))
              (do
                (ra/dispatch-to-subscribers dispatch subscribers (js/Date.))
                (recur subscribers))))))
      ch-ctrl)))

(defn every [interval msg]
  (ra/subscription (->Time interval) msg))
