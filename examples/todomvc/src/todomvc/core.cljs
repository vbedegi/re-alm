(ns todomvc.core
  (:require-macros [cljs.core.match :refer [match]])
  (:require [cljs.core.match :as m]
            [clojure.string :as s]
            [re-alm.boot :as boot]
            [re-alm.core :as ra]
            [re-alm.utils :refer [log]]))

(defn mmap [m f a] (->> m (f a) (into (empty m))))

(defn add-todo [model text]
  (let [id (inc (:counter model))
        todos' (assoc (:todos model) id {:id id :title text :done false :editing? false})]
    (assoc model
      :counter id
      :todos todos')))

(defn complete-all [model v]
  (update model :todos mmap map #(assoc-in % [1 :done] v)))

(defn clear-done [model]
  (update model :todos mmap remove #(get-in % [1 :done])))

(defn toggle [model id]
  (update-in model [:todos id :done] not))

(defn save [model id title]
  (update model :todos assoc-in [id :title] title))

(defn delete [model id]
  (update model :todos dissoc id))

(defn clear-input [model]
  (assoc model :input ""))

(defn start-editing [model id]
  (-> model
      (assoc-in [:todos id :editing?] true)
      (assoc-in [:todos id :input] (get-in model [:todos id :title]))))

(defn- init-todo []
  (-> {:todos   (sorted-map)
       :counter 0
       :filt    :all
       :input   ""}
      (add-todo "Rename Cloact to Reagent")
      (add-todo "Add undo demo")
      (add-todo "Make all rendering async")
      (add-todo "Allow any arguments to component functions")
      (complete-all true)))

(defn todo-input [value dispatch messages props]
  (let []
    [:input
     (merge props
            {:type        "text"
             :value       value
             :placeholder "What needs to be done?"
             :on-change   #(dispatch (ra/tag (:on-change messages) (-> % .-target .-value str)))
             :on-blur     #(dispatch (:on-save messages))
             :on-key-down (fn [e]
                            (case (.-which e)
                              13 (dispatch (:on-save messages))
                              27 (dispatch (:on-cancel messages))
                              nil))})]))

(defn todo-item [{:keys [id done title editing?] :as model} dispatch]
  [:li {:class (str (if done "completed ")
                    (if editing? "editing"))}
   [:div.view
    [:input.toggle {:type      "checkbox"
                    :checked   done
                    :on-change #(dispatch [:toggle id])}]
    [:label {:on-double-click #(dispatch [:start-editing id])} title]
    [:button.destroy {:on-click #(dispatch [:delete id])}]]
   (when editing?
     [todo-input
      (:input model) dispatch
      {:on-change [:input-changed id]
       :on-save   [:save-input id]
       :on-cancel [:cancel-input id]}
      {:class "edit"}])])

(defn todo-stats [{:keys [filt active done]} dispatch]
  (let [props-for (fn [name]
                    {:class    (if (= name filt) "selected")
                     :on-click #(dispatch [:set-filt name])})]
    [:div
     [:span#todo-count
      [:strong active] " " (case active 1 "item" "items") " left"]
     [:ul#filters
      [:li [:a (props-for :all) "All"]]
      [:li [:a (props-for :active) "Active"]]
      [:li [:a (props-for :done) "Completed"]]]
     (when (pos? done)
       [:button#clear-completed
        {:on-click #(dispatch :clear-done)}
        "Clear completed " done])]))

(defn- render-todo [model dispatch]
  (let [items (-> model :todos vals)
        done (->> items (filter :done) count)
        active (- (count items) done)]
    [:div
     [:section#todoapp
      [:header#header
       [:h1 "todos"]
       [todo-input
        (:input model) dispatch
        {:on-change :input-changed
         :on-save   :save-input
         :on-cancel :cancel-input}
        {:id "new-todo"}]]
      (when (-> items count pos?)
        [:div
         [:section#main
          [:input#toggle-all
           {:type      "checkbox"
            :checked   (zero? active)
            :on-change #(dispatch [:complete-all (pos? active)])}]
          [:label {:for "toggle-all"} "Mark all as complete"]
          [:ul#todo-list
           (for [todo (filter (case (:filt model)
                                :active (complement :done)
                                :done :done
                                :all identity) items)]
             ^{:key (:id todo)} [todo-item todo dispatch])]]
         [:footer#footer
          [todo-stats {:active active :done done :filt (:filt model)} dispatch]]])]
     [:footer#info
      [:p "Double-click to edit a todo"]]]))

(defn- update-todo [model msg]
  (match msg
         [:complete-all v]
         (complete-all model v)

         :clear-done
         (clear-done model)

         [:set-filt v]
         (assoc model :filt v)

         [:toggle id]
         (toggle model id)

         [:delete id]
         (delete model id)

         [:input-changed v]
         (assoc model :input v)

         [:input-changed id v]
         (assoc-in model [:todos id :input] v)

         :save-input
         (let [input (s/trim (:input model))]
           (if (empty? input)
             model
             (-> model
                 (add-todo input)
                 clear-input)))

         [:save-input id]
         (let [input (s/trim (get-in model [:todos id :input]))]
           (if (empty? input)
             model
             (-> model
                 (assoc-in [:todos id :title] input)
                 (assoc-in [:todos id :editing?] false))))

         :cancel-input
         (clear-input model)

         [:cancel-input id]
         (assoc-in model [:todos id :editing?] false)

         [:start-editing id]
         (start-editing model id)

         _
         model))

(def todo-component
  {:render #'render-todo
   :update #'update-todo})

(boot/boot
  (.getElementById js/document "app")
  todo-component
  (init-todo))
