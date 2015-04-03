(ns filmster.server
  (:require [filmster.dev :refer [is-dev? inject-devmode-html start-figwheel browser-repl]]
            [compojure.core :refer [GET defroutes]]
            [compojure.handler :refer [api]]
            [ring.middleware.reload :as reload]
            [environ.core :refer [env]]
            [org.httpkit.server :refer [run-server]]
            [clojure.java.io :as io]
            [compojure.route :refer [resources]]
            [net.cgrand.enlive-html :refer [deftemplate]]
            [filmster.api :as api]))

(deftemplate page
  (io/resource "index.html") [] [:body] (if is-dev? inject-devmode-html identity))

(defroutes routes
  (resources "/")
  (resources "/react" {:root "react"})
  (GET "/movies/" [& params] (api/movie-query params))
  (GET "/movie-details/" [& params] (api/detail-movie-query params))
  (GET "/events/" [] (api/festival-query))
  (GET "/awards/" [] (api/award-query))
  (GET "/years/" [] (api/year-query))
  (GET "/movie/by-director/:term/" [term] (api/itunes-link term))
  (GET "/" req (page)))

(def http-handler
  (if is-dev?
    (reload/wrap-reload (api #'routes))
    (api routes)))

(defn run [& [port]]
  (defonce ^:private server
    (do
      (if is-dev? (start-figwheel))
      (let [port (Integer. (or port (env :port) 10555))]
        (print "Starting web server on port" port ".\n")
        (run-server http-handler {:port port
                                  :join? false}))))
  server)

(defn -main [& [port]]
  (run port))
