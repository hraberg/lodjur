(ns lodjur.client
  (:require [clojure.walk :as walk]))

;; from http://stackoverflow.com/a/10196356, originally from http://mmcgrana.github.com/2011/09/clojurescript-nodejs.html
(defn clj->js [x]
  "Recursively transforms ClojureScript maps into Javascript objects,
   other ClojureScript colls into JavaScript arrays, and ClojureScript
   keywords into JavaScript strings.

   Borrowed and updated from mmcgrana."
  (cond
    (string? x) x
    (keyword? x) (name x)
    (map? x) (.-strobj (reduce (fn [m [k v]]
               (assoc m (clj->js k) (clj->js v))) {} x))
    (coll? x) (apply array (map clj->js x))
    :else x))

(defn eval-clj [clj]
  (let [clj (if (string? clj) clj (pr-str clj))]
    (js->clj (.parse js/JSON (js/evalClojure clj)))))

(defn apply-clj [f args]
  (let [args (map #(.stringify js/JSON (lodjur.client/clj->js %)) args)]
    (js->clj (.parse js/JSON (apply js/applyClojure (str f) args)))))

(set! cljs.core/*print-fn* (fn [& more]
                             (lodjur.client/apply-clj 'println more)
                             nil))

(defn dbg [x]
  (println x)
  x)
