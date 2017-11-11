(ns nested.core
  (:require-macros [cljs.core.match :refer [match]])
  (:require [cljs.core.match :as m]
            [re-alm.core :as ra]
            [re-alm.io.time :as t]))

(defn make-indexed [xs]
  (map-indexed (fn [idx x] [idx x]) xs))

(defn sum-counter [model]
  (->> (:nested-counters model)
       (map sum-counter)
       (reduce + (:value model))))

(defn- init-counter []
  {:value                0
   :nested-counters      []
   :interested-in-ticks? false
   :ticks-so-far         0})

(defn- render-counter [model dispatch]
  [:div.well
   {:style {:padding "4px" :margin "2px"}}
   [:div
    [:button {:on-click #(dispatch :inc)} "inc"]
    [:button {:on-click #(dispatch :dec)} "dec"]
    [:span.p2 "value:"]
    [:span.p2.lead (:value model)]
    [:span.p2 "total:"]
    [:span.p2.lead (sum-counter model)]
    [:span.p2]
    [:span {:style {:padding "2px"}}]
    [:button {:on-click #(dispatch :add-nested-counter)} "add nested counter"]
    [:span.p2 "interested in ticks?"]
    [:input {:type      "checkbox"
             :checked   (:interested-in-ticks? model)
             :on-change #(dispatch :toggle-interested-in-ticks)}]
    [:span.p2 "ticks so far:"]
    [:span (:ticks-so-far model)]]
   [:div.container {:style {:padding "2px"}}
    (for [[idx nested-counter] (make-indexed (:nested-counters model))]
      ^{:key idx} [render-counter nested-counter (ra/tag-dispatch dispatch [:nested idx])]
      ;                                           ^^^ We make a dispatcher, which will tag the
      ;                                               dispatched message with the provided taggers.
      ;                                               Eg., if the view dispatches the message :inc,
      ;                                               it will become [:nested 0 :inc], and so on.
      )]])


(defn- update-counter [model msg]
  (match msg
         :inc
         (update model :value inc)

         :dec
         (update model :value dec)

         :add-nested-counter
         (update model :nested-counters conj (init-counter))

         [:nested idx sub-msg]
         (ra/forward model [:nested-counters idx] update-counter sub-msg [:nested idx])
         ;           1     2                      3              4       5
         ; We forward the payload of the message to a sub-component.
         ;   1, current model
         ;   2, accessor for the sub-component's model (note, this accessor is used to read _and_ write the sub-component's model)
         ;   3, update fn of the sub-component
         ;   4, message to forward
         ;   5, taggers for effects made by the sub-component (taggers =~ message routing information)

         :toggle-interested-in-ticks
         (update model :interested-in-ticks? not)

         [:tick _]
         (update model :ticks-so-far inc)

         _
         model))

; Subcriptions expresses interest in events from the outside world.
; Given a current model, the subscriptions fn returns current interests of the component.
; In a nested component scenario, we also collect the nested component's subscriptions.
(defn subscriptions [model]
  (concat
    (when (:interested-in-ticks? model)
      [(t/every 1000 :tick)])
    (->>
      (for [[idx nested-counter] (make-indexed (:nested-counters model))]
        ; The sub-component's subscriptions are tagged, that is, enriched with message routing information.
        (ra/forward-subs (subscriptions nested-counter) [:nested idx]))
      (mapcat identity))))

(def counter-component
  {:render        #'render-counter
   :update        #'update-counter
   :subscriptions #'subscriptions})
