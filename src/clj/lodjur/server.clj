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
            [cljs.compiler :as compiler])
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
   #(proxy [BrowserFunction] [browser name]
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
                                        :locals '{}}
                                       form)))

(defn eval-cljs [cljs]
  (js (compile-cljs (list 'lodjur.client/clj->js (walk/macroexpand-all cljs)))))

(defmacro cljs [cljs]
  `(let [env# (zipmap '~(keys &env) ~(vec (keys &env)))]
     (eval-cljs (walk/postwalk-replace env# '~cljs))))

(defmacro $ [& [fst & rst]]
  `(eval-cljs (-> (js/jQuery ~fst) ~@rst)))

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