(ns fastgoodcheap.boot
  (:require
    [re-alm.boot :as boot]
    [re-alm.core :as ra]
    [fastgoodcheap.core :as core]))

(enable-console-print!)

(defn ^:export init []
  (boot/boot
    (.getElementById js/document "app")
    core/counter-component
    (core/init-fastgoodcheap)
    (-> ra/default-handler
        ra/wrap-failsafe)))


