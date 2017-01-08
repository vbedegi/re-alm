(ns re-alm.boot
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [re-alm.core :as ra]))

(def dispatch (ra/make-dispatcher :re-alm/root-component))

(def default-db {})

(rf/register-handler
  :re-alm/init
  (fn [db [_ root-component]]
    (assoc default-db
      :re-alm/root-component root-component
      :re-alm/event-manager (ra/set-subs
                              (ra/->EventManager dispatch)
                              (ra/get-subscriptions root-component (:model root-component))))))

(rf/register-sub
  :re-alm/root-component
  (fn [db _]
    (reaction
      (get @db :re-alm/root-component))))

(defn- app-view []
  (let [root-component (rf/subscribe [:re-alm/root-component])]
    (ra/render-component @root-component dispatch)))

(def ^{:dynamic true} *renderer* (fn []))

(defn render []
  (*renderer*))

(defn boot [container component model]
  (rf/dispatch-sync [:re-alm/init (assoc component :model model)])
  (set! *renderer* #(r/render [app-view] container))
  (render))