# re-alm

An [Elm Architecture](https://guide.elm-lang.org/architecture/) experiment in ClojureScript, powered by [Reagent](http://reagent-project.github.io/) and [re-frame](https://github.com/Day8/re-frame).
 
## Why?

- You want a ~~easy~~ simple way to have local state, and parent/children relation between components.
- You want to have pure message handler functions, and put side effects somewhere else (note, this project was started back when there were no notion of effects in re-frame. It is still based on 0.7.0)

## Usage

[![Clojars Project](https://img.shields.io/clojars/v/re-alm.svg)](https://clojars.org/re-alm)

```clojure
(defn init-counter []
  0)
  
(defn render-counter [model dispatch]
  [:div (str model)
    [:button {:on-click #(dispatch :inc)} "increment"]
    [:button {:on-click #(dispatch :dec)} "decrement"]])
    
(defn update-counter [model msg]
  (match msg
         :inc
         (inc model)

         :dec
         (dec model)

         _
         model))
```

In the render function you get the model, and a function to `dispatch` your messages with. The messages are automatically routed to your update function. 

In the update function you get the actual model, and the message you should handle. I prefer to use [core.match](https://github.com/clojure/core.match) here, but it is optional. The result is the new model, or the model and side effects (more on that later)

And finally, you got to bootstrap your app.

 ```clojure
 (boot
   (.getElementById js/document "app")
   {:update #'update-counter
    :render #'render-counter}
   (init-counter))
```

## Advanced usage

The result of the update function can be the new model, or possibly some side effects attached to it.

```clojure
(defn update [model msg]
  (match msg
         :go-and-fetch-some-data
         (with-fx
           (assoc model :loading? true)
           (http/get-fx "/url" {:params {:foo :bar}} :fetch-done))

         [:fetch-done data]
         (assoc model
                :loading? false
                :data data)
                
         _
         model))
```

Every time the model changes, the optionally provided `subscriptions` function is called, where you can describe what external events you are interested in.

```clojure
(defn update-foo [model msg]
  (match msg
    [:tick _]
    (update model :ticks-so-far inc)))

(defn subscriptions [model]
  [(time/every 1000 :tick)])

(def foo-component
  {:update        #'update-foo
   :render        #'render-foo
   :subscriptions #'subscriptions})
```

Middlewares are following the concept you may already know from Ring. The middleware function takes a component (the root component of your app), and the message being dispatched. It returns a tuple (vector) of the new version of the model, the side effects made during the update, and the subscriptions your model currently interested in.

```clojure
(defn wrap-log [handler]
  (fn [component msg]
    (.log js/console msg)
    (handler component msg)))

(boot
   (.getElementById js/document "app")
   component
   model
   (-> ra/handler
       ra/wrap-log))
```

Parent/children communication example

```clojure
; TODO
```

Using websockets

```clojure
(defn- update-ws [model msg]
  (match msg
         :send-msg
         (ra/with-fx
           model
           (ws/websocket-fx "/ws" "payload"))

         [:ws m]
         (update-in model [:messages] conj m)

         _
         model))

(defn- subscriptions [model]
  [(ws/websocket "/ws" :ws)])

(def ws-controller
  {:render        #'render-ws
   :update        #'update-ws
   :subscriptions #'subscriptions})
```

## Roadmap (just a bunch of what-ifs)

- the effets (http, ws, storage) may go into a separate package
- the boot process is a mess, needs some cleanup

## License

The license is MIT.

