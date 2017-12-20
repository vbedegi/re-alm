(ns re-alm.io.browser
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [goog.events :as events]
            [cljs.core.async :as async]
            [re-alm.core :as ra])
  (:import [goog.history Html5History EventType]))

(defrecord FocusFx [selector]
  ra/IEffect
  (execute [this dispatch]
    (when-let [elem (.querySelector js/document selector)]
      (.focus elem))))

(defn focus-fx [selector]
  (->FocusFx selector))

; inspiration from http://www.lispcast.com/mastering-client-side-routing-with-secretary-and-goog-history

(defn- make-history []
  (doto (Html5History.)
    (.setPathPrefix (str js/window.location.protocol
                         "//"
                         js/window.location.host))
    (.setUseFragment true)))

(defonce ^:private history (doto
                             (make-history)
                             (.setEnabled true)))

(defrecord NavigateFx [url]
  ra/IEffect
  (execute [this dispatch]
    (.setToken history url)))

(defn navigate-fx [url]
  (->NavigateFx url))

(defrecord Navigate []
  ra/ITopic
  (make-event-source [this dispatch subscribers]
    (let [ch-ctrl (async/chan)
          ch-events (async/chan)]
      (events/listen history EventType.NAVIGATE #(async/put! ch-events %))
      (go
        (loop [subscribers subscribers]
          (let [[v ch] (async/alts! [ch-ctrl ch-events])]
            (if (= ch ch-ctrl)
              (if (not= v :kill)
                (recur (second v)))
              (do
                (ra/dispatch-to-subscribers dispatch subscribers (.-token v))
                (recur subscribers))))))
      ch-ctrl)))

(defn navigate [msg]
  (ra/subscription (->Navigate) msg))

(defrecord OpenUrlFx [url]
  ra/IEffect
  (execute [this dispatch]
    (.open js/window url "_blank")))

(defn open-url-fx [url]
  (->OpenUrlFx url))

(defrecord PathCoFx []
  ra/ICoEffect
  (extract-value [this]
    (.getToken (Html5History.))))

(defn path-cofx []
  (->PathCoFx))