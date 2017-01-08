(ns re-alm.io.http
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [reagent.ratom :refer [reaction]])
  (:require [ajax.core :as ajax]
            [re-alm.core :as ra]))

(defrecord GetFx [url options done fail]
  ra/IEffect
  (execute [this dispatch]
    (let [http-options {:params          (:params options)
                        :response-format (ajax/json-response-format {:keywords? true})
                        :handler         (fn [resp]
                                           (->> resp (ra/build-msg done) dispatch))}]
      (ajax/GET url http-options)))
  ra/ITaggable
  (tag-it [this tagger]
    (-> this
        (cond-> done
                (update :done conj tagger))
        (cond-> fail
                (update :fail conj tagger)))))

(defn get-fx
  ([url done]
   (get-fx url {} done))
  ([url options done]
   (->GetFx url options [done] nil)))
