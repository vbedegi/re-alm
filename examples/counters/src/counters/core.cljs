(ns counters.core
  (:require-macros [cljs.core.match :refer [match]])
  (:require [cljs.core.match :as m]
            [re-alm.core :as ra]
            [re-alm.boot :as boot]))

(enable-console-print!)

(defn make-indexed [coll]
  (map-indexed (fn [idx item] [idx item]) coll))

; ---

(defn- init-counter []
  0)

(defn reset-counter [model]
  0)

(defn- render-counter [model dispatch]
  [:div
   {:style {:padding-bottom "10px"}}
   [:span.lead
    model]
   [:button.btn.btn-default {:on-click #(dispatch :inc)} "inc"]
   [:button.btn.btn-default {:on-click #(dispatch :dec)} "dec"]])

(defn- update-counter [model msg]
  (match msg
         :inc
         (inc model)

         :dec
         (dec model)

         _
         model))

; ---

(defn- init-counterlist []
  {:counters [0 1]})

(defn add-counter [model]
  (update model :counters conj (init-counter)))

(defn- render-counterlist [model dispatch]
  [:div
   {:style {:padding "10px"}}
   (for [[idx counter] (-> model :counters make-indexed)]
     ^{:key idx}
     [render-counter counter (ra/tag-dispatch dispatch [:counter idx])])
   [:p.lead "sum: " (apply + (:counters model))]
   [:button.btn.btn-default {:on-click #(dispatch :add-counter)} "add counter"]
   [:button.btn.btn-default {:on-click #(dispatch :reset-all)} "reset all"]])

(defn- update-counterlist [model msg]
  (match msg
         :add-counter
         (add-counter model)

         :reset-all
         (update model :counters #(mapv reset-counter %))

         [:counter idx sub-msg]
         (ra/forward model [:counters idx] update-counter sub-msg [:counters idx])

         _
         model))

(def counterlist-component
  {:render #'render-counterlist
   :update #'update-counterlist
   :start  :add-counter})

(boot/boot
  (.getElementById js/document "app")
  counterlist-component
  (init-counterlist))
