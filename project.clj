(defproject re-alm "0.3.6-SNAPSHOT"
  :description "An Elm Architecture experiment in ClojureScript"
  :url "https://github.com/vbedegi/re-alm"
  :license {:name "MIT"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.9.946"]
                 [org.clojure/core.async "0.3.465"]
                 [reagent "0.7.0"]
                 [cljs-ajax "0.7.3"]
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
