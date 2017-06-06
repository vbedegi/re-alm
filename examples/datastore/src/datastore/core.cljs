(ns datastore.core
  (:require-macros [cljs.core.match :refer [match]])
  (:require [cljs.core.match :as m]
            [re-alm.core :as ra]
            [re-alm.io.datastore :as ds]))

(def names ["Bob" "Alice" "Foo" "Bar"])

(def messages ["hello" "hi" "yo"])

(defn make-indexed [coll]
  (map-indexed (fn [idx item] [idx item]) coll))

; ----------------------------------------------

(defn- init-participant [name]
  {:name        name
   :global-data nil})

(defn- render-participant [model dispatch]
  [:div.panel.panel-default
   [:div.panel-heading
    (:name model)]
   [:button.btn.btn-default {:on-click #(dispatch :write-datastore)} "write datastore"]
   [:div.panel-body
    (:data model)]])

(defn update-participant [model msg]
  (match msg
         :write-datastore
         (ra/with-fx
           model
           (ds/write-store-fx :global-data (rand-int 100)))

         [:datastore-value-changed x]
         (assoc model :data x)

         _
         model))

(defn subscriptions-participant [model]
  [(ds/datastore :global-data :datastore-value-changed)])

; ----------------------------------------------

(defn- participants-taggers [model]
  (for [[idx _] (make-indexed (:participants model))]
    [:participants idx]))

(defn- init-container []
  {:participants [(init-participant (names 0))]})

(defn- render-container [model dispatch]
  [:div
   [:div
    [:button.btn.btn-default {:on-click #(dispatch :add-participant)} "add participant"]
    [:button.btn.btn-default {:on-click #(dispatch :remove-participant)} "remove participant"]]
   [:div
    (for [[idx participant] (make-indexed (:participants model))]
      ^{:key idx}
      [:div {:style {:float  "left"
                     :width  "250px"
                     :margin "2px"}}
       [render-participant participant (ra/tag-dispatch dispatch [:participants idx])]])]])

(defn- update-container [model msg]
  (match msg
         [:participants idx sub-msg]
         (ra/forward model [:participants idx] update-participant sub-msg [:participants idx])

         :add-participant
         (let [cnt (count (:participants model))]
           (if (< cnt 4)
             (update model :participants conj (init-participant (nth names cnt)))
             model))

         :remove-participant
         (let [cnt (count (:participants model))]
           (if (pos? cnt)
             (update model :participants #(vec (drop-last %)))
             model))

         _
         model))

(defn subscriptions-container [model]
  (->> (participants-taggers model)
       (map #(ra/forward-subs model % subscriptions-participant %))
       (mapcat identity)
       vec))

; ----------------------------------------------

(def container-component
  {:render        #'render-container
   :update        #'update-container
   :subscriptions #'subscriptions-container})

