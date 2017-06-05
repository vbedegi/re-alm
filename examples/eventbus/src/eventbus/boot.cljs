(ns eventbus.boot
  (:require
    [re-alm.boot :as boot]
    [re-alm.core :as ra]
    [eventbus.core :as core]))

(enable-console-print!)

(defn ^:export init []
      (boot/boot
        (.getElementById js/document "app")
        core/container-component
        (core/init-container)
        (-> ra/handler
            ra/wrap-log)))
