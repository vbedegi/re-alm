(defproject re-alm "0.2.0"
  :description "An Elm Architecture experiment in ClojureScript"
  :url "https://github.com/vbedegi/re-alm"
  :license {:name "MIT"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.518"]
                 [org.clojure/core.async "0.3.442"]
                 [reagent "0.6.1"]
                 [re-frame "0.7.0"]
                 [cljs-ajax "0.5.9"]
                 [jarohen/chord "0.8.1"]
                 [alandipert/storage-atom "2.0.1"]]

  :plugins [[lein-cljsbuild "1.1.4"]]

  :source-paths ["src"]
  :test-paths ["test"]

  :clean-targets ^{:protect false} ["run/compiled" "target" "test/js"]

  :cljsbuild {:builds [{:id           "dev"
                        :source-paths ["src"]
                        :compiler     {:output-to            "run/compiled/re-alm.js"
                                       :output-dir           "run/compiled/out"
                                       :source-map-timestamp true
                                       :optimizations        :none
                                       :pretty-print         true}}]})
