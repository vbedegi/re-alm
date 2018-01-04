(ns re-alm.io.http
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [reagent.ratom :refer [reaction]])
  (:require [cljs.core.async :as async]
            [ajax.core :as ajax]
            [re-alm.core :as ra]))

(defn GET [url options]
  (let [response-ch (async/chan)
        http-options {:params          (:params options)
                      :headers         (:headers options)
                      :response-format (ajax/json-response-format {:keywords? true})
                      :handler         (fn [resp]
                                         (async/put! response-ch (ra/ok resp)))
                      :error-handler   (fn [error]
                                         (async/put! response-ch (ra/error error)))}]
    (ajax/GET url http-options)
    response-ch))

(defn POST [url options]
  (let [response-ch (async/chan)
        http-options {:params          (:params options)
                      :headers         (:headers options)
                      :format          (ajax/json-request-format)
                      :response-format (ajax/json-response-format {:keywords? true})
                      :handler         (fn [resp]
                                         (async/put! response-ch (ra/ok resp)))
                      :error-handler   (fn [error]
                                         (async/put! response-ch (ra/error error)))}]
    (ajax/POST url http-options)
    response-ch))

(defn DELETE [url options]
  (let [response-ch (async/chan)
        http-options {:params          (:params options)
                      :headers         (:headers options)
                      :format          (ajax/json-request-format)
                      :response-format (ajax/json-response-format {:keywords? true})
                      :handler         (fn [resp]
                                         (async/put! response-ch (ra/ok resp)))
                      :error-handler   (fn [error]
                                         (async/put! response-ch (ra/error error)))}]
    (ajax/DELETE url http-options)
    response-ch))


(defrecord GetFx [url options message]
  ra/IEffect
  (execute [this dispatch]
    (go
      (let [resp (async/<! (GET url options))
            msg (ra/build-msg this message resp)]
        (dispatch msg)))))

(defn get-fx
  ([url message]
   (get-fx url {} message))
  ([url options message]
   (->GetFx url options message)))

(defrecord PostFx [url options message]
  ra/IEffect
  (execute [this dispatch]
    (go
      (let [resp (async/<! (POST url options))
            msg (ra/build-msg this message resp)]
        (dispatch msg)))))

(defn post-fx
  ([url message]
   (post-fx url {} message))
  ([url options message]
   (->PostFx url options message)))

(defrecord DeleteFx [url options message]
  ra/IEffect
  (execute [this dispatch]
    (go
      (let [resp (async/<! (POST url options))
            msg (ra/build-msg this message resp)]
        (dispatch msg)))))

(defn delete-fx
  ([url message]
   (delete-fx url {} message))
  ([url options message]
   (->DeleteFx url options message)))