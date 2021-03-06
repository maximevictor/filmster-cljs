(ns filmster.api
  (:require [cheshire.core :as json]
            [filmster.itunes :as itunes]
            [filmster.countries :as countries]
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

(defn countries-query []
  (json-response countries/countries))

(defn itunes-link [term]
  (json-response {:itunes-link (itunes/build-itunes-api-search-url "uk" "directorTerm" term)}))

(defn detail-movie-query [{:keys [director original-title english-title] :as params}]
  (json-response (itunes/get-cached-itunes-result director original-title english-title)))
