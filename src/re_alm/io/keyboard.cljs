(ns re-alm.io.keyboard
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [goog.events :as events]
            [cljs.core.async :refer [put! chan <!] :as async]
            [re-alm.core :as ra]))

(defn- listen [el type]
  (let [out (chan)]
    (events/listen el type #(put! out %))
    out))

(def ^private key-name {8   :backspace
                        13  :enter
                        27  :escape
                        32  :space
                        33  :pageup
                        34  :pagedown
                        35  :end
                        36  :home
                        38  :up
                        40  :down
                        37  :left
                        39  :right
                        45  :insert
                        46  :delete
                        112 :f1
                        113 :f2
                        114 :f3
                        115 :f4
                        116 :f5
                        117 :f6
                        118 :f7
                        119 :f8
                        120 :f9
                        121 :f10
                        122 :f11
                        123 :f12})

(defn- should-process? [key-event {:keys [key] :as args}]
  (let [target (.-target key-event)
        nodeName (.-nodeName target)]
    (or
      (contains? #{:escape} key)
      (and
        (not= "INPUT" nodeName)
        (not= "SPAN" nodeName)))))

(defrecord KeyDowns []
  ra/ITopic
  (make-event-source [this dispatch subscribers]
    (let [ch-ctrl (async/chan)
          ch-keydowns (listen js/document.body "keydown")]
      (go
        (loop [subscribers subscribers]
          (let [[v ch] (async/alts! [ch-ctrl ch-keydowns])]
            (if (= ch ch-ctrl)
              (if (not= v :kill)
                (recur (second v)))
              (do
                (let [key-event v
                      key-code (.-keyCode key-event)
                      args {:key-code key-code
                            :alt?     (.-altKey key-event)
                            :ctrl?    (.-ctrlKey key-event)
                            :shift?   (.-shiftKey key-event)
                            :key      (get key-name key-code)}]
                  (when (should-process? key-event args)
                    (ra/dispatch-to-subscribers dispatch subscribers args)))

                (recur subscribers))))
          ))
      ch-ctrl)))

(defn key-downs [msg]
  (ra/subscription (->KeyDowns) msg))

(defrecord KeyPresses []
  ra/ITopic
  (make-event-source [this dispatch subscribers]
    (let [ch-ctrl (async/chan)
          ch-keydowns (listen js/document.body "keypress")]
      (go
        (loop [subscribers subscribers]
          (let [[v ch] (async/alts! [ch-ctrl ch-keydowns])]
            (if (= ch ch-ctrl)
              (if (not= v :kill)
                (recur (second v)))
              (do
                (let [key-event v
                      key-code (.-keyCode key-event)
                      char-code (.-charCode key-event)
                      args {:key-code  key-code
                            :char-code char-code
                            :char      (.fromCharCode js/String char-code)
                            :alt?      (.-altKey key-event)
                            :ctrl?     (.-ctrlKey key-event)
                            :shift?    (.-shiftKey key-event)
                            :key       (get key-name key-code)}]
                  (when (should-process? key-event args)
                    (ra/dispatch-to-subscribers dispatch subscribers args)))

                (recur subscribers))))
          ))
      ch-ctrl)))

(defn key-presses [msg]
  (ra/subscription (->KeyPresses) msg))