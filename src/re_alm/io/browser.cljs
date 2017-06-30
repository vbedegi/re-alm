(ns re-alm.io.browser
  (:require [re-alm.core :as ra]))

(defrecord FocusFx [selector]
  ra/IEffect
  (execute [this dispatch]
    (when-let [elem (.querySelector js/document selector)]
      (.focus elem))))

(defn focus-fx [selector]
  (->FocusFx selector))