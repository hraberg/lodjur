(defproject lodjur "0.1.0-SNAPSHOT"
  :description "SWT Browser & ClojureScript"
  :dependencies [[org.clojure/clojure "1.5.0-alpha2"]
                 [org.clojure/clojurescript "0.0-1236" :exclusions [org.apache.ant/ant]]
                 [cheshire "4.0.0"]
                 [compojure "1.0.4"]
                 [ring/ring-jetty-adapter "1.1.1"]]
  :profiles {:dev {:dependencies [[org.eclipse.swt/org.eclipse.swt.gtk.linux.x86_64 "3.7.2"]]}
             :linux {:dependencies [[org.eclipse.swt/org.eclipse.swt.gtk.linux.x86_64 "3.7.2"]]}
             :win32 {:dependencies [[org.eclipse.swt/org.eclipse.swt.win32.win32.x86_64 "3.7.2"]]}
             :macosx {:dependencies [[org.eclipse.swt/org.eclipse.swt.cocoa.macosx.x86_64 "3.7.2"]]}}
  :min-lein-version "2.0.0"
  :source-paths ["src" "src/clj"]
  :plugins [[lein-swank "1.4.4"]
            [lein-cljsbuild "0.2.1"]]
  :cljsbuild  {:builds
               [{:source-path "src/cljs",
                 :compiler
                 {:output-to "resources/public/client.js"
                  :optimizations :simple}}]}
  :main lodjur.server
  :repositories [["swt"
                  "http://swt-repo.googlecode.com/svn/repo/"]])