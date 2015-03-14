(ns filmster.api
  (:require [cheshire.core :as json]
            [filmster.itunes :as itunes]
            [filmster.films :as films]
            ))

(defn json-response [data & [status]]
  {:status  (or status 200)
   :headers {"Content-Type" "application/hal+json; charset=utf-8"}
   :body    (json/generate-string data)})

(defn movie-query [params]
  (json-response (films/get-data params)))

(defn festival-query []
  (json-response (films/get-festivals)))

(defn award-query []
  (json-response (films/get-awards)))

(defn year-query []
  (json-response (films/get-years-bounds)))

(defn itunes-link [term]
  (json-response {:itunes-link (itunes/construct-itunes-search term "uk" nil)}))
