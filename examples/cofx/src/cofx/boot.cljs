(ns cofx.boot
  (:require
    [re-alm.boot :as boot]
    [re-alm.core :as ra]
    [cofx.core :as core]))

(enable-console-print!)

(defn ^:export init []
  (boot/boot
    (.getElementById js/document "app")
    core/counter-component
    (core/init-counter)
    (-> ra/default-handler
        ra/wrap-coeffect)))


