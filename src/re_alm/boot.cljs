(ns re-alm.boot
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [re-alm.core :as ra]))

(def dispatch (ra/make-dispatcher :re-alm/root-component))

(rf/register-handler
  :re-alm/init
  (fn [db [_ root-component handler]]
    {:re-alm/root-component root-component
     :re-alm/event-manager  (ra/set-subs
                              (ra/->EventManager dispatch)
                              (ra/get-subscriptions (:subscriptions root-component) (:model root-component)))
     :re-alm/handler        handler}))

(rf/register-sub
  :re-alm/root-component
  (fn [db _]
    (reaction
      (get @db :re-alm/root-component))))

(defn- app-view []
  (let [root-component (rf/subscribe [:re-alm/root-component])]
    [ra/render-component @root-component dispatch]))

(def ^{:dynamic true} *renderer* (fn []))

(defn render []
  (*renderer*))

(defn boot
  ([container component model]
   (boot container component model ra/handler))
  ([container component model handler]
   (rf/dispatch-sync [:re-alm/init (assoc component :model model) handler])
   (set! *renderer* #(r/render [app-view] container))
   (render)))