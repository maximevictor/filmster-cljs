(ns filmster.itunes
  (:require [clj-http.client :as http]
            [clj-diff.core :as diff]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [filmster.redis :as store]
            ))

(def AFFILIATE-LINK "at=1l3vvZJ")

(defn build-itunes-api-search-url [country attribute term]
  "builds the query string for the iTunes API.
   NOTE: seemingly, one can only query iTunes on a single dimension at once, director name OR movie title"

  (let [itunes-root "https://itunes.apple.com/search"
        query       {:media     "movie"
                     :entity    "movie"
                     :country   (or country "US")
                     :attribute attribute
                     :term      term}
        form-query  (ring.util.codec/form-encode query)]
    (str itunes-root "?" form-query)))

;; "https://itunes.apple.com/search?term=%@&media=movie&entity=movie&attribute=directorTerm&country=%@", uri-encode(directorName) countryCode

(defn query-itunes! [director country]
  (let [url      (build-itunes-api-search-url country "directorTerm" director)
        response (http/get url {:as :json})
        results  (->> response :body :results)]
    results))

(defn strip-movie-result-title [movie-string]
  "removes text within parentheses from movie string"
  (clojure.string/replace movie-string #"\([^)]*\)" ""))

(defn get-diff-scores-for-movies [movie-results target-title]
  "generates similarity scores for each movie result with the target movie title
   (using levenshtein distances)"
  (->> movie-results
       (map :trackName)
       (map strip-movie-result-title)
       (map (partial diff/levenshtein-distance target-title))))

(defn find-index-of-best-result [diff-scores threshold]
  "returns vector index of best result,
   or -1 if threshold isn't met"
  (if (< 0 (count diff-scores))
    (let [indexed-vector-of-scores (map-indexed vector diff-scores)
          [best-idx best-score]    (apply min-key second indexed-vector-of-scores)]
      (if (<= best-score threshold)
        best-idx
        -1))
    -1))

(defn get-closest-itunes-result [{:keys [director movie-title country]}]

  (try
    (let [movie-results   (query-itunes! director
                                         country)
          diff-scores     (get-diff-scores-for-movies movie-results
                                                      movie-title) 
          best-result-idx (find-index-of-best-result diff-scores
                                                     10)]
      (if (< 0 best-result-idx)
        (nth movie-results best-result-idx)
        nil))
    (catch Exception e nil)))

(defn cache-wrapper [fn args key]
  "returns value at key, or computes fn and stores result at key
   NOTE: stores empty results as empty map instead of nil, in order to cache them too"
  (let [cache-value (store/get-key key)]
    (if cache-value
      (do
        (prn "cache hit")
        cache-value)
      (let [result (fn args)]
        (store/set-key key (or result {}))
        result))))

(defn get-cached-itunes-result [director movie-title]
  (let [args     {:director    director
                  :movie-title movie-title
                  :country     "US"}
        hash-key (str director movie-title)]
  (cache-wrapper get-closest-itunes-result args hash-key)))

(defn append-affiliate-code-to-link [link]
  (str link "&" AFFILIATE-LINK))

(defn append-metadata-to-film [{:keys [originalTitle englishTitle director] :as film}]
  (let [title                  (or originalTitle englishTitle)
        itunes-data            (get-cached-itunes-result director title)
        affiliated-itunes-link (append-affiliate-code-to-link (:trackViewUrl itunes-data))]
    (if (not (empty? itunes-data))
      (assoc film
             :image (:artworkUrl100 itunes-data)
             :itunes-url affiliated-itunes-link)
      film)))





