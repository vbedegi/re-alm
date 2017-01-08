(ns clock.core
  (:require-macros [cljs.core.match :refer [match]])
  (:require [cljs.core.match :as m]
            [re-alm.io.time :as t]
            [re-alm.boot :as boot]))

(defn- init-clock []
  0)

(defn deg->rad [d]
  (/ (* Math/PI d) 180))

(defn- render-clock [model dispatch]
  (let [angle (deg->rad (- (* model 6) 90))
        hand-x (+ 50 (* 40 (Math/cos angle)))
        hand-y (+ 50 (* 40 (Math/sin angle)))]
    [:div
     [:svg
      {:view-box "0 0 100 100"
       :width    "300px"}
      [:circle {:cx 50 :cy 50 :r 45 :fill "#0B79CE"}]
      [:line {:x1 50 :y1 50 :x2 hand-x :y2 hand-y :stroke "#023963"}]]]))

(defn- update-clock [model msg]
  (match msg
         [:tick time]
         (.getSeconds time)

         _
         model))

(defn subscriptions [model]
  [(t/every 1000 :tick)])

(def clock-component
  {:render        #'render-clock
   :update        #'update-clock
   :subscriptions #'subscriptions})

(boot/boot
  (.getElementById js/document "app")
  clock-component
  (init-clock))