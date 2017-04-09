(ns re-alm.utils
  (:require [clojure.set :as s]))

(defn log [& messages]
  (apply println messages))

(defn conj-in [m ks v]
  (update-in m ks #(conj % v)))

(defn update-vals [f m]
  (reduce (fn [acc [k v]]
            (assoc acc k (f k v)))
          {}
          m))

(defn diff-maps [m1 m2]
  (let [keys1 (set (keys m1))
        keys2 (set (keys m2))]
    {:missing  (s/difference keys1 keys2)
     :new      (s/difference keys2 keys1)
     :modified (set (filter #(not= (get m1 %) (get m2 %)) (s/intersection keys1 keys2)))}))
