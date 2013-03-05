# SWT Browser (or JavaFX) & ClojureScript

*Added a similar [JavaFX spike](https://github.com/hraberg/lodjur/blob/master/src/clj/lodjur/javafx.clj) based on https://github.com/raju-bitter/clojure-javafx-example Needs `javafxrt.jar`, see Raju's README.*

I often find myself coming back to the [SWT Browser](http://www.eclipse.org/swt/snippets/#browser).

**This spike** is not necessarily intended to evolve into a standalone project, but ideas may get used in [Deuce](https://github.com/hraberg/deuce) or future projects at some point.


I'm not sure this approach - apps with embedded browsers as (part of) their UI - is good, but its compelling.
ClojureScript isn't the real goal, but this area seemed like a good time to explore it a bit closer. Some of the use of it here is likely somewhat idiosyncratic.

In the unlikely event you want to run it, pick your SWT dev platform (see `project.clj`) and run `lein deps`, compile `lodjur.server` and run `(start)`. You should get a SWT (WebKit) browser running in the same process as your REPL (usually Swank).

The macro `js` evaluates JavaScript strings, while the macro `cljs` evaluates ClojureScript forms. `$` is a convenience macro for jQuery:

```clojure
(js "$('h2').text('SWT & JS')")
(cljs (-> (js/$ "h2") (.text "SWT & Clojure")))
($ :h2 (text "SWT & jQuery"))
```

Inside the browser there are two "native" functions [registered](http://git.eclipse.org/c/platform/eclipse.platform.swt.git/tree/examples/org.eclipse.swt.snippets/src/org/eclipse/swt/snippets/Snippet307.java), `evalClojure` and `applyClojure`, which allows to evaluate real Clojure on the host from JavaScript:

```clojure
(cljs (js/alert (js/evalClojure "(System/getProperty \"java.version\")")))
; displays an alert with the JVM Clojure version in the browser.

($ "<a href=#>Clojure</a>" (appendTo :body) (click (fnc (println :click))))
; clicking this link outputs ":click" in the Clojure REPL.

($ [:a {:href "#"} "Hiccup"] (appendTo :body) (click (fnc (println :click))))
; same as above, but using (Clojure-side) Hiccup
```

`fnc` is a macro to register a Clojure callback function in ClojureScript.

All different calls currently return JSON for simplicity (not everything in the browser is Clojure). There's no AJAX, but Jetty is in there somewhere to serve resources.

For distribution OSGi/RCP could reluctantly be brought into the mix, or simply build platform specific uberjars like this:

    lein with-profile linux uberjar
    java -jar target/lodjur-0.1.0-SNAPSHOT-standalone.jar


## Things to Explore

* Data binding between browser UI and Clojure state.
* Functional Reactive Programming.
* Leverage existing JavaScript UI frameworks.
* ClojureScript vs. JavaScript


## Links

[JavaFX and Web Integration](http://www.slideshare.net/kazuchika/english-version-javafx-and-web-integration) "One of the key features of JavaFX 2.0 is having full-fledged embedded browser."

[JavaFX WebEngine](http://docs.oracle.com/javafx/2/api/javafx/scene/web/WebEngine.html)

[Krypton](https://github.com/thoughtworks/krypton) is an experimental (SWT) browser testing tool by ThoughtWorks Studios.

[Himera](https://github.com/fogus/himera) Fogus' "ClojureScript compiler as service" - I started here, but wanted to get a feel for the moving pieces of as its a spike.

[Seesaw](https://github.com/daveray/seesaw) Looks nice - if one wants Swing.

[Closeout](https://github.com/davesann/closeout) Clojurescript UI templating and binding

[Elm](http://elm-lang.org/) "Elm is a type-safe, functional reactive language that compiles to HTML, CSS, and JavaScript."

[TodoMVC](https://github.com/addyosmani/todomvc/) "A common learning application for popular JavaScript MV* frameworks"


### Name

lodjur is the Swedish word for [lynx](http://lynx.browser.org/), but that's just a lucky coincidence after picking a word for this spike at random in a [Transtromer](http://www.guardian.co.uk/books/2011/oct/06/nobel-prize-literature-tomas-transtromer) collection.


## License

Copyright (C) 2012 Hakan Raberg

Distributed under the Eclipse Public License, the same as Clojure.
