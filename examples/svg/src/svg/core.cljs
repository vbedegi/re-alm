(ns svg.core
  (:require-macros [cljs.core.match :refer [match]])
  (:require [clojure.string :as str]
            [cljs.core.match :as m]
            [re-alm.core :as ra]
            [re-alm.boot :as boot]
            [re-alm.io.time :as time]))

(enable-console-print!)

(defn- make-path [cx cy w c]
  (let [right (* c w 2)]
    (str
      (str/join
        " "
        (for [i (range c)]
          (str "M " (* i (* w 2)) "," 0 " L " cx "," cy " L" (+ (* i (* w 2)) w) "," 0 " Z")))
      " "
      (str/join
        " "
        (for [i (range c)]
          (str "M " (- right (* i (* w 2))) "," right " L " cx "," cy " L" (- right (+ (* i (* w 2)) w)) "," right " Z")))
      " "
      (str/join
        " "
        (for [i (range c)]
          (str "M " 0 "," (+ w (* i (* w 2))) " L " cx "," cy " L" 0 "," (+ w w (* i (* w 2))) " Z")))
      " "
      (str/join
        " "
        (for [i (range c)]
          (str "M " right "," (- right (* i (* w 2)) w) " L " cx "," cy " L" right "," (- right (+ (* i (* w 2)) w w)) " Z"))))))

(def PI 3.141592653589793)

(defn deg->rad [deg]
  (* deg (/ PI 180)))

(defn sin [p]
  (.sin js/Math (deg->rad (mod p 360))))

(defn cos [p]
  (.cos js/Math (deg->rad (mod p 360))))

(defn- init-svg []
  {:pointers [0 0 0 0]})

(defn- render-svg [model dispatch]
  [:div
   {:style {:padding "40px"}}
   (let [[p1x p1y p2x p2y] (:pointers model)
         c1x (+ 200 (* 180 (sin p1x)))
         c1y (+ 200 (* 180 (cos p1y)))
         c2x (+ 200 (* 180 (sin p2x)))
         c2y (+ 200 (* 180 (cos p2y)))]
     [:svg
      {:style {:width  "400px"
               :height "400px"
               :border "thin solid #ededed"}}
      [:path
       {:d         (str (make-path c1x c1y 20 10)
                        " "
                        (make-path c2x c2y 20 10))
        :fill-rule "evenodd"
        :fill      "grey"}]])])

(defn- update-svg [model msg]
  (match msg

         [:tick v]
         (let [[p1x p1y p2x p2y] (:pointers model)]
              (assoc model :pointers [(+ p1x 1)
                                      (+ p1y 2)
                                      (+ p2x 3)
                                      (+ p2y 4)]))

         _
         model))


(defn subscriptions [model]
  [(time/every 16 :tick)])

(def svg-component
  {:render        #'render-svg
   :update        #'update-svg
   :subscriptions #'subscriptions})

(boot/boot
  (.getElementById js/document "app")
  svg-component
  (init-svg))
