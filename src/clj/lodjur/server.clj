(ns lodjur.server
  (:use [cheshire.core]
        [compojure.core :only (defroutes GET)]
        [ring.util.response :only (redirect)]
        [compojure.route :only (resources)]
        [ring.adapter.jetty])
  (:import [org.eclipse.swt SWT SWTException]
           [org.eclipse.swt.widgets Display Shell]
           [org.eclipse.swt.layout FillLayout]
           [org.eclipse.swt.browser Browser TitleListener BrowserFunction])
  (:require [clojure.string :as string]
            [clojure.walk :as walk]
            [cljs.compiler :as compiler]
            [hiccup.core])
  (:gen-class))

(def ^:dynamic *use-browser* :webkit)
(def ^:dynamic *port* 8080)

;; SWT Browser

(declare display shell browser)

(defn swt-event-loop []
  (while (not (.isDisposed shell))
    (try
      (when-not (.readAndDispatch display)
        (.sleep display))
      (catch Exception _))))

(defn sync-exec [f]
  (let [result (atom nil)]
    (.syncExec display #(reset! result (f)))
    @result))

(defn async-exec [f]
  (.asyncExec display f))

(defn goto [url]
  (async-exec #(.setUrl browser (str url))))

(defn browser-fn [name f]
  (sync-exec
   #(proxy [BrowserFunction] [browser (str name)]
      (function [^"[Ljava.lang.Object;" args]
        (apply f (seq args))))))

(defn show-ui []
  (def shell (doto (Shell. display)
               (.setLayout (FillLayout.))
               (.setText "Loading...")))

  (def browser (doto (Browser. shell ({:mozilla SWT/MOZILLA
                                       :webkit SWT/WEBKIT}
                                      *use-browser*))
                 (.addTitleListener
                  (proxy [TitleListener] []
                    (changed [e] (.setText shell (.title e)))))))

  (browser-fn "evalClojure" (comp generate-string eval read-string))
  (browser-fn "applyClojure" (fn [f & args]
                               (generate-string
                                (apply (resolve (symbol f))
                                       (map #(parse-string % true) args)))))

  (.open shell)
  (goto (str "http://localhost:" *port* "/")))

(defn toggle-fullscreen []
  (async-exec #(.setFullScreen shell (not (.getFullScreen shell)))))

(defn start-display []
  (.start (Thread.
           (fn []
             (def display (Display/getDefault))
             (show-ui)
             (swt-event-loop)))))

;; ClojureScript

(defn js [js]
  (try
    (-> #(.evaluate browser (str "return JSON.stringify(" js ");"))
        sync-exec
        (parse-string true))
    (catch SWTException e
      (-> e .getCause .getMessage println))))

(defn compile-cljs [form]
  (compiler/emit-str (compiler/analyze {:ns (@compiler/namespaces 'cljs.core)
                                        :uses #{'cljs.core}
                                        :context :expr
                                        :locals {}}
                                       form)))

(create-ns 'lodjur.client)
(create-ns 'js)

(defn eval-cljs [cljs]
  (js (compile-cljs (list 'lodjur.client/clj->js (walk/macroexpand-all cljs)))))

(defmacro cljs [cljs]
  `(let [env# (zipmap '~(keys &env) ~(vec (keys &env)))]
     (eval-cljs (walk/postwalk-replace env# '~cljs))))

(defn compile-callback [src]
  (eval (read-string src)))

(alter-var-root #'compile-callback memoize)

(defn callback [src & args]
  (apply (compile-callback src) args))

(defmacro fnc
  ([clj]
     (if (some '#{%} (flatten clj))
       `(fnc [~'%] ~clj)
       `(fnc [] ~(if (symbol? clj) (list clj) clj))))
  ([args & clj]
     (let [src (pr-str `(fn ~args ~@clj))]
       `(fn ~args
          (lodjur.client/apply-clj 'callback (cons ~src ~args))))))

(defn jq-expand [x & [add-dot?]]
  (condp some [x]
    (every-pred
     symbol?
     (constantly add-dot?)) (symbol (str "." x))
     keyword? (name x)
     vector? (apply print-str (map jq-expand x))
     x))

(defmacro dom [dom]
  `(hiccup.core/html ~dom))

(defmacro $ [& expr]
  `(let [env# (zipmap '~(keys &env) ~(vec (keys &env)))
         [fst# & rst#] (map #(if (list? %)
                               (cons (jq-expand (first %) :add-dot)
                                     (map jq-expand (rest %)))
                               (jq-expand % :add-dot))
                            (walk/macroexpand-all
                             (walk/postwalk-replace env# '~expr)))]
     (eval-cljs `(-> (js/jQuery ~fst#) ~@rst#))))

;; HTTP

(defroutes handler
  (GET "/" [] (redirect "/index.html"))

  (resources "/"))

(def app handler)

(defn start-server []
  (def server (run-jetty #'app {:port *port* :join? false})))

(defn start []
  (start-server)
  (start-display))

(defn -main [& args]
  (start))