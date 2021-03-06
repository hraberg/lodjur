;; Based on https://github.com/raju-bitter/clojure-javafx-example
;;      and https://github.com/hraberg/lodjur

;; First, install javafxrt.jar in your local maven repo:
;; ./mvn-install-javafx

;; lein repl
;; (require 'lodjur.javafx)
;; (in-ns 'lodjur.javafx)
;; (start)

;; An empty WebView will show up, press Ctrl-C in the REPL go get back a prompt:

;; (render [:h2 "Clojure & JavaFX"]) ;; Hiccup
;; (eval-cljs '(+ 2 2))              ;; ClojureScript

;; (render [:a {:href "#"} "Click me"])
;; (on :click (first (node-seq body)) (comp println bean)) ;; WebEngine callback from WebKit to Java.

;; See http://docs.oracle.com/javafx/2/api/javafx/scene/web/WebEngine

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
           [org.w3c.dom Node NodeList]
           [org.w3c.dom.events EventListener Event])
  (:gen-class :extends javafx.application.Application))

(defn node-seq [x]
  (let [nodes (if (instance? NodeList x) x (.getChildNodes x))]
    (for [idx (range (.getLength nodes))]
      (.item nodes idx))))

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

(defn xpath
  ([path] (xpath body path))
  ([element path]
     (let [result (.evaluate document path element nil (short 0) nil)]
       (take-while identity (repeatedly #(.iterateNext result))))))

;; Doesn't work
(defn css [selector]
  (node-seq (.querySelectorAll document selector)))

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
