;; Based on https://github.com/raju-bitter/clojure-javafx-example
;;      and https://github.com/hraberg/lodjur
(ns lodjur.javafx
  (:require [hiccup.core :as h]
            [clojure.string :as s]
            [clojure.edn :as edn]
            [clojure.walk :as w]
            [clojure.java.io :as io]
            [cljs.compiler]
            [cljs.analyzer])
  (:import [javafx.application Platform Application]
           [javafx.scene Scene]
           [javafx.scene.layout StackPane]
           [javafx.stage Stage]
           [javafx.scene.web WebView]
           [clojure.lang Seqable]
           [org.w3c.dom Node]
           [org.w3c.dom.events EventListener Event])
  (:gen-class :extends javafx.application.Application))

(defn node-seq [x]
  (loop [x (.getFirstChild x)
         acc []]
    (if x
      (recur (.getNextSibling x) (cons x acc))
      acc)))

(defn compile-cljs [form]
  (cljs.compiler/emit-str (cljs.analyzer/analyze
                           {:ns (@cljs.analyzer/namespaces 'cljs.core)
                            :uses #{'cljs.core}
                            :context :expr
                            :locals {}}
                           form)))

(declare engine document body)

(defn run-later [f]
  (Platform/runLater f))

(defn run-now [f]
  (let [latch (promise)]
    (run-later #(deliver latch (f)))
       @latch))

(defn render
  ([html] (render body html))
  ([element html]
     (run-now #(doto element (.setMember "innerHTML" (h/html html))))))

(defn new-document []
  (.call (.getMember document "implementation") "createHTMLDocument" (object-array 0)))

(defn parse-html
  [html] (.getMember (render (.getMember (new-document) "body") (h/html html)) "firstChild"))

(defn on [event element listener]
  (run-now #(doto element
              (.addEventListener
               (name event)
               (proxy [EventListener] []
                 (handleEvent [^Event e] (listener e))) false))))

(defn eval-js
  ([js] (eval-js body js))
  ([element js] (.eval element js)))

(defn eval-cljs [cljs]
  (eval-js (compile-cljs (w/macroexpand-all cljs))))

(defn eval-cljs-edn [cljs]
  (read-string (eval-cljs (list `pr-str (w/macroexpand-all cljs)))))

(defn -start [this ^Stage stage]
  (let [root (StackPane.)
        web-view (WebView.)]

    (def engine (.getEngine web-view))
    (def document (.getValue (.documentProperty engine)))
    (def body (.getBody document))

    (eval-js (slurp (io/resource "cljs.js")))

    (.add (.getChildren root) web-view)
    (.setScene stage (Scene. root 800 600))
    (.show stage)))

(defn -stop [this])

(defn start []
  (Application/launch lodjur.javafx (into-array String [])))

(defn -main [& args]
  (start))
