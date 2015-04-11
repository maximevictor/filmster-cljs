(ns filmster.api
  (:require [cheshire.core :as json]
            [filmster.itunes :as itunes]
            [filmster.query :as query]))

(defn json-response [data & [status]]
  {:status  (or status 200)
   :headers {"Content-Type" "application/hal+json; charset=utf-8"}
   :body    (json/generate-string data)})

(defn movie-query [params]
  (json-response (query/query-films params)))


(defn festival-query []
  (json-response (query/get-events)))

(defn award-query []
  (json-response (query/get-awards)))

(defn year-query []
  (json-response (query/get-years-bounds)))

(defn itunes-link [term]
  (json-response {:itunes-link (itunes/construct-itunes-search "uk" "directorTerm" term)}))

(defn detail-movie-query [{:keys [director movie-title] :as params}]
  (json-response (itunes/get-cached-itunes-result director movie-title)))
