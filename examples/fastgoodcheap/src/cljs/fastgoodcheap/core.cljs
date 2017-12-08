(ns fastgoodcheap.core
  (:require-macros [cljs.core.match :refer [match]])
  (:require [clojure.set :as s]
            [cljs.core.match :as m]
            [reagent.core :as r]
            [re-alm.core :as ra]))

(def react-toggle (aget js/window "deps" "react-toggle"))

(def Toggle
  (r/adapt-react-class (.-default react-toggle)))

(defn choice [k v checked dispatch]
  [:div
   [:label v]
   [Toggle {:checked   checked
            :on-change #(dispatch [:toggle k (.-checked (.-target %))])}]])

(defn- init-fastgoodcheap []
  {:choices         [[:fast "Fast"]
                     [:good "Good"]
                     [:cheap "Cheap"]]
   :toggled-on      #{}
   :last-toggled-on []})

(defn- render-fastgoodcheap [model dispatch]
  [:div
   (let [toggled-on (:toggled-on model)]
     (for [[k v] (:choices model)]
       ^{:key k} [choice k v (toggled-on k) dispatch]))])

(defn- update-fastgoodcheap [model msg]
  (match msg
         [:toggle k v]
         (let [last-toggled-on (:last-toggled-on model)
               last-toggled-on (if v
                                 (->> (conj last-toggled-on k)
                                      (take-last 2)
                                      vec)
                                 last-toggled-on)
               toggled-on (:toggled-on model)
               toggled-on (if v
                            (conj toggled-on k)
                            (disj toggled-on k))
               toggled-on (s/intersection toggled-on (set last-toggled-on))]
           (assoc model :last-toggled-on last-toggled-on :toggled-on toggled-on))

         _
         model))

(def counter-component
  {:render #'render-fastgoodcheap
   :update #'update-fastgoodcheap})
