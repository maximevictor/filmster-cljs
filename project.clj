(defproject filmster "0.1.0"
  :description "filmster web app"
  :source-paths ["src/clj" "src/cljs"]

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]

                 [ring "1.3.2"]
                 [compojure "1.2.0"]
                 [enlive "1.1.5"]
                 [cheshire "5.4.0"]
                 [clj-http "1.0.1"]
                 [http-kit "2.1.19"]
                 [shodan "0.4.1"]
                 [org.clojure/data.json "0.2.5"]
                 [org.clojure/tools.logging "0.3.1"]
                 [clj-diff "1.0.0-20110104.084027-9"]
                 [com.taoensso/carmine "2.9.2"]

                 ;; [org.clojure/clojurescript "0.0-3126" :scope "provided"]
                 [org.clojure/clojurescript "0.0-2843" :scope "provided"]
                 ;; [org.omcljs/om "0.8.8"]
                 [om "0.7.3"]
                 [prismatic/om-tools "0.3.3" :exclusions [org.clojure/clojure]]
                 [domina "1.0.3"]
                 [cljs-ajax "0.3.10"]
                 [secretary "1.2.3"]

                 [figwheel "0.2.5"]
                 [environ "1.0.0"]
                 [com.cemerick/piggieback "0.1.5"]
                 [weasel "0.6.0"]
                 [leiningen "2.5.0"]
                 [debugger "0.1.4"]]

  :plugins [[lein-cljsbuild "1.0.3"]
            [lein-environ "1.0.0"]
            [lein-figwheel "0.2.5"]]

  :min-lein-version "2.5.0"

  :uberjar-name "filmster.jar"

  :figwheel {:css-dirs ["resources/public/css"]
             :server-logfile "log/server.log"}

  :cljsbuild {:builds {:app {:source-paths ["src/cljs"]
                             :compiler {:output-to     "resources/public/js/app.js"
                                        :output-dir    "resources/public/js/out"
                                        :source-map    "resources/public/js/out.js.map"
                                        :preamble      ["react/react.min.js"]
                                        :externs       ["react/externs/react.js"]
                                        :optimizations :none
                                        :pretty-print  true}}}}

  :profiles {:dev {:repl-options {:init-ns          filmster.server
                                  :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

                   :plugins [[lein-figwheel "0.2.5"]]

                   :figwheel {:http-server-root "public"
                              :port 3449
                              :css-dirs ["resources/public/css"]}

                   :env {:is-dev true}

                   :cljsbuild {:builds {:app {:source-paths ["env/dev/cljs"]}}}}

             :uberjar {:hooks [leiningen.cljsbuild]
                       :env {:production true}
                       :omit-source true
                       :aot :all
                       :cljsbuild {:builds {:app
                                            {:source-paths ["env/prod/cljs"]
                                             :compiler
                                             {:optimizations :advanced
                                              :pretty-print false}}}}}})
