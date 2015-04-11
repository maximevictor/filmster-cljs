(ns filmster.itunes
  (:require [clj-http.client :as http]
            [clj-diff.core :as diff]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [filmster.redis :as store]
            ))

(defn construct-itunes-search [country attribute term]
  (let [itunes-root  "https://itunes.apple.com/search"
        query {:media     "movie"
               :entity    "movie"
               :country   country
               :attribute attribute
               :term      term}
        form-query (ring.util.codec/form-encode query)]
    (str itunes-root "?" form-query)))

;; "https://itunes.apple.com/search?term=%@&media=movie&entity=movie&attribute=directorTerm&country=%@", uri-encode(directorName) countryCode

(defn query-itunes [director]
  (let [response (http/get (construct-itunes-search "US" "directorTerm" director)
                           {:as :json})
        results  (->> response :body :results)]
    results))

(defn strip-movie-result-title [movie-string]
  (clojure.string/replace movie-string #"\([^)]*\)" ""))

(defn get-closest-itunes-result [director movie-title]
  (let [results            (query-itunes director)
        movie-names        (map :trackName results)
        candidates         (map strip-movie-result-title movie-names)
        application        (partial diff/levenshtein-distance movie-title)
        leverstein-scores  (map application candidates)
        leverstein-kv      (map-indexed vector leverstein-scores)
        best-candidate-idx (first (apply min-key second leverstein-kv))
        best-candidate     (nth results best-candidate-idx)
        ]
    best-candidate))


(defn get-cached-itunes-result [director movie-title]
  (log/info movie-title)
  (let [store-key (str director movie-title)
        cache     (store/get-key store-key)]
    (if cache
      (do
        (log/info "cache hit")
        cache)
      (let [result (get-closest-itunes-result director movie-title)]
        (store/set-key store-key result)
        result))))

