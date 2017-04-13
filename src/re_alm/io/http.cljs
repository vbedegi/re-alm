(ns re-alm.io.http
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [reagent.ratom :refer [reaction]])
  (:require [cljs.core.async :as async]
            [ajax.core :as ajax]
            [re-alm.core :as ra]))

(defn GET [url options]
  (let [response-ch (async/chan)
        http-options {:params          (:params options)
                      :response-format (ajax/json-response-format {:keywords? true})
                      :handler         (fn [resp]
                                         (async/put! response-ch resp))}]
    (ajax/GET url http-options)

    response-ch))

(defrecord GetFx [url options done fail]
  ra/IEffect
  (execute [this dispatch]
    (go
      (let [resp (async/<! (GET url options))
            msg (ra/build-msg done resp)]
        (dispatch msg))))
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
