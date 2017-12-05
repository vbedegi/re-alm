(ns cofx.core
  (:require-macros [cljs.core.match :refer [match]])
  (:require [cljs.core.match :as m]
            [re-alm.core :as ra]
            [re-alm.io.time :as rat]
            [re-alm.io.random :as rar]))

(defn build-message []
  [:action (rat/now-cofx) (rar/rand-int-cofx 1000)])

(defn- init-counter []
  {})

(defn- render-counter [model dispatch]
  [:div
   {:style {:padding-bottom "10px"}}
   [:div "time: " (str (or (:time model) "---"))]
   [:div "rand-value: " (str (or (:rand-value model) "---"))]
   [:button {:on-click #(dispatch (build-message))} "action!"]])

(defn- update-counter [model msg]
  (match msg
         [:action time rand-value]
         (assoc model :time time :rand-value rand-value)

         _
         model))

(def counter-component
  {:render #'render-counter
   :update #'update-counter})
