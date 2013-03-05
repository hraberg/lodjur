(defproject lodjur "0.1.0-SNAPSHOT"
  :description "SWT Browser & ClojureScript"
  :dependencies [[org.clojure/clojure "1.5.0"]
                 [org.clojure/clojurescript "0.0-1586"]
                 [cheshire "4.0.0"]
                 [compojure "1.0.4"]
                 [hiccup "1.0.2"]
                 [ring/ring-jetty-adapter "1.1.1"]
                 ;; JavaFX dependency has to be installed manually.
                 [local.oracle/javafxrt "2.2.0"]]
  :profiles {:dev {:dependencies [[org.eclipse.swt/org.eclipse.swt.gtk.linux.x86_64 "3.8"]]}
             :linux {:dependencies [[org.eclipse.swt/org.eclipse.swt.gtk.linux.x86_64 "3.8"]]}
             :win32 {:dependencies [[org.eclipse.swt/org.eclipse.swt.win32.win32.x86_64 "3.8"]]}
             :macosx {:dependencies [[org.eclipse.swt/org.eclipse.swt.cocoa.macosx.x86_64 "3.8"]]}}
  :min-lein-version "2.0.0"
  :source-paths ["src" "src/clj"]
  :plugins [[lein-swank "1.4.4"]
            [lein-cljsbuild "0.2.1"]]
  :cljsbuild  {:builds
               [{:source-path "src/cljs",
                 :compiler
                 {:output-to "resources/public/client.js"
                  :optimizations :simple}}]}
  :aot [lodjur.javafx]
  :main lodjur.server
  :repositories [["swt"
                  "http://swt-repo.googlecode.com/svn/repo/"]])