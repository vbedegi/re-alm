(ns re-alm.io.random
  (:require [re-alm.core :as ra]))

(defrecord RandIntCoFx [n]
  ra/ICoEffect
  (extract-value [this]
    (rand-int n)))

(defn rand-int-cofx [n]
  (->RandIntCoFx n))

(defrecord UUIDCoFx []
  ra/ICoEffect
  (extract-value [this]
    (random-uuid)))

(defn uuid-cofx []
  (->UUIDCoFx))