(ns re-alm.boot
  (:require [reagent.core :as r]
            [re-alm.core :as ra]))

(defn- app-view [app]
  [ra/render-component @(:component app) (:dispatch app)])

(def ^{:dynamic true} *renderer* (fn []))

(defn render []
  (*renderer*))

(defn boot
  ([container component model]
   (boot container component model ra/handler))
  ([container component model handler]
   (let [component (assoc component :model model)
         app (ra/make-app handler component)]
     (ra/run-app app)
     (set! *renderer* #(r/render [app-view app] container))
     (render))))