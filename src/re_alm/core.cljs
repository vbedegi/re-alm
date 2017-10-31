(ns re-alm.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async]
            [reagent.core :as r]
            [re-alm.utils :refer [log conj-in update-vals diff-maps]]))

; tagging

(defprotocol ITagger
  (tag [this value]))

(extend-protocol ITagger
  nil
  (tag [this value]
    value)
  Keyword
  (tag [this value]
    [this value])
  PersistentVector
  (tag [this value]
    (conj this value))
  IFn
  (tag [this value]
    (this value)))

(defn make-tagger-fn [tagger]
  (fn [value]
    (tag tagger value)))

(defprotocol ITaggable
  (tag-it [this tagger]))

(extend-type default
  ITaggable
  (tag-it [this tagger]
    this))

(defn build-msg [taggers payload]
  (reduce (fn [payload tagger]
            (tag tagger payload))
          payload
          taggers))

; update result

(defprotocol IUpdateResult
  (extract-model [this])
  (extract-effects [this]))

(extend-type default
  IUpdateResult
  (extract-model [this] this)
  (extract-effects [this] []))

(defrecord ResultWithEffects [model effects]
  IUpdateResult
  (extract-model [this] model)
  (extract-effects [this] effects))

(defn with-fx [update-result & effects]
  (->ResultWithEffects
    (extract-model update-result)
    (concat (extract-effects update-result) (remove nil? effects))))

; lens

(defprotocol ILens
  (read [this model])
  (write [this model value]))

(extend-type Keyword
  ILens
  (read [this model]
    (get model this))
  (write [this model value]
    (assoc model this value)))

(extend-type PersistentVector
  ILens
  (read [this model]
    (get-in model this))
  (write [this model value]
    (assoc-in model this value)))

(defrecord DelegateLens [read-fn write-fn]
  ILens
  (read [this model]
    (read-fn model))
  (write [this model value]
    (write-fn model value)))

(defn delegate-lens [read-fn write-fn]
  (->DelegateLens read-fn write-fn))

;

(defn ok [value]
  {:ok value})

(defn error [error]
  {:error error})

; effect

(defprotocol IEffect
  (execute [this dispatch]))

(defn- execute-effects [effects dispatch]
  (doseq [effect effects]
    (execute effect dispatch)))

(defrecord DispatchFx [msg options taggers]
  IEffect
  (execute [this dispatch]
    (let [message (build-msg taggers msg)]
      (if-let [delay (:delay options)]
        (go
          (async/<! (async/timeout delay))
          (dispatch message))
        (dispatch message))))
  ITaggable
  (tag-it [this tagger]
    (update this :taggers conj tagger)))

(defn dispatch-fx
  ([msg]
   (dispatch-fx msg {}))
  ([msg options]
   (->DispatchFx msg options [])))

;; ---

(defn- get-subscriptions [subscriptions-fn model]
  (if subscriptions-fn
    (->> model subscriptions-fn (remove nil?) vec)
    []))

(defn dispatch-to-subscribers [dispatch subscribers payload]
  (doseq [subscriber subscribers]
    (->> payload (build-msg subscriber) dispatch)))

(defprotocol ITopic
  (make-event-source [this dispatch subscribers]))

(defprotocol ISubscription
  (add-tagger [this tagger]))

(defrecord Subscription [topic taggers]
  ISubscription
  (add-tagger [this tagger]
    (update this :taggers conj tagger)))

(defn subscription [topic msg]
  (->Subscription topic [msg]))

(defprotocol IEventManager
  (set-subs [this subs]))

(defrecord EventManager [dispatch]
  IEventManager
  (set-subs [this subs]
    (let [old-subs-by-topic (get this :taggers-by-topic {})
          new-subs-by-topic (->> subs
                                 (group-by :topic)
                                 (update-vals (fn [k v] (mapv :taggers v))))
          {missing-topics  :missing
           modified-topics :modified
           new-topics      :new
           :as             diffs} (diff-maps old-subs-by-topic new-subs-by-topic)

          old-event-sources (get this :event-sources-by-topic {})

          new-event-sources (if (empty? missing-topics)
                              old-event-sources
                              (do
                                (doseq [missing-topic missing-topics]
                                  (let [event-source (get old-event-sources missing-topic)]
                                    (go (async/>! event-source :kill))))
                                (reduce (fn [event-sources topic]
                                          (dissoc event-sources topic))
                                        old-event-sources
                                        missing-topics)))
          new-event-sources (if (empty? modified-topics)
                              new-event-sources
                              (do
                                (doseq [modified-topic modified-topics]
                                  (let [event-source (get new-event-sources modified-topic)]
                                    (go (async/>! event-source [:subscribers (vec (get new-subs-by-topic modified-topic))]))))
                                new-event-sources))
          new-event-sources (if (empty? new-topics)
                              new-event-sources
                              (reduce (fn [event-sources topic]
                                        (assoc event-sources topic (make-event-source topic dispatch (get new-subs-by-topic topic))))
                                      new-event-sources
                                      new-topics))]
      (-> this
          (assoc :taggers-by-topic new-subs-by-topic)
          (assoc :event-sources-by-topic new-event-sources)))))

(defn forward-subs [model sub-model-lens sub-subscribe-fn tagger]
  (let [sub-model (read sub-model-lens model)
        sub-subscriptions (->> (sub-subscribe-fn sub-model) (remove nil?))
        sub-subscriptions' (map #(add-tagger % tagger) sub-subscriptions)]
    sub-subscriptions'))

; ---

(defn- step [update-fn model msg msg-tagger]
  (let [update-result (update-fn model msg)
        model' (extract-model update-result)
        effects (extract-effects update-result)
        tagged-effects (mapv #(tag-it % msg-tagger) effects)]
    [model' tagged-effects]))

(defn forward [model sub-model-lens sub-update-fn sub-msg msg-tagger]
  (if-let [sub-model (read sub-model-lens model)]
    (let [[sub-model' effects] (step sub-update-fn sub-model sub-msg msg-tagger #_(make-tagger-fn msg-tagger))
          model' (write sub-model-lens model sub-model')]
      (if (empty? effects)
        model'
        (apply with-fx model' effects)))
    (do
      (log "!! missed forward !!")
      model)))

(defn -tag-dispatch [dispatch tagger]
  (fn [msg]
    (->> msg (tag tagger) dispatch)))

(def tag-dispatch (memoize -tag-dispatch))

(def component-id (atom 0))

(defn make-component [template model]
  (assoc template
    :model model
    :id (str (swap! component-id inc))))

(defn render-component [{:keys [render model]} dispatch]
  (if render
    [render model dispatch]
    (log "no render fn provided!")))

(defn identify [component]
  (:id component))

(defn handler [{model            :model
                update-fn        :update
                subscriptions-fn :subscriptions}
               msg]
  (let [[model effects] (step update-fn model msg nil)
        subscriptions (get-subscriptions subscriptions-fn model)]
    [model effects subscriptions]))

(defn wrap-log [handler]
  (fn [component msg]
    (.log js/console msg)
    (handler component msg)))

; ---

(defn- make-dispatch [ch]
  (fn [msg]
    (go
      (async/>! ch msg))))

(defn make-app [handler component]
  (let [dispatch-ch (async/chan)]
    {:dispatch-ch dispatch-ch
     :handler     handler
     :component   (r/atom component)
     :dispatch    (make-dispatch dispatch-ch)}))

(defn run-app [{:keys [dispatch-ch component dispatch] :as app}]
  (let [event-manager (set-subs
                        (->EventManager dispatch)
                        (get-subscriptions (:subscriptions @component) (:model @component)))]
    (go
      (loop [app app
             event-manager event-manager]
        (let [msg (async/<! dispatch-ch)]
          (let [handler (:handler app)
                component-v @component
                [model effects subscriptions] (handler component-v msg)
                component-v' (assoc component-v :model model)
                _ (when (not= component-v component-v')
                    (reset! component component-v'))
                event-manager' (set-subs event-manager subscriptions)]
            (execute-effects effects dispatch)
            (recur app event-manager')))))))
