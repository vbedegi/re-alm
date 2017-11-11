(ns nested.boot
  (:require
    [re-alm.boot :as boot]
    [nested.core :as core]))

(enable-console-print!)

(defn ^:export init []
  (boot/boot
    (.getElementById js/document "app")
    core/counter-component
    (core/init-counter)))


