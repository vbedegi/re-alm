# re-alm

An [Elm Architecture](https://guide.elm-lang.org/architecture/) experiment in ClojureScript, powered by [Reagent](http://reagent-project.github.io/) and [re-frame](https://github.com/Day8/re-frame).
 
## Why?

- You want a ~~easy~~ simple way to have local state, and parent/children relation between components.
- You want to have pure message handler functions, and put side effects somewhere else (note, this project was started back when there were no notion of effects in re-frame. It is still based on 0.7.0)

## Usage

```clj
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

 ```clj
 (boot
   (.getElementById js/document "app")
   {:update #'update-counter
    :render #'render-counter}
   (init-counter))
```

## Advanced usage

The result of the update function can be the new model, or possibly some side effects attached to it.

```clj
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

```clj
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

Parent/children communication example: TODO

## License

The license is MIT.

