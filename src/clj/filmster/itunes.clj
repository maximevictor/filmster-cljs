(ns filmster.itunes
  (:require [clj-http.client :as http]
            [clj-diff.core :as diff]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [clojure.string :as string]
            [filmster.cache :as cache]
            [ring.util.codec :as codec]
            [filmster.utils :as utils]))

(def AFFILIATE-LINK "at=1l3vvZJ")

(defn append-affiliate-code-to-link [link]
  (str link "&" AFFILIATE-LINK))

(defn build-itunes-api-search-url [country attribute term]
  "builds the query string for the iTunes API.
   NOTE: seemingly, one can only query iTunes on a single dimension at once, director name OR movie title"

  (let [itunes-root "https://itunes.apple.com/search"
        query       {:media     "movie"
                     :entity    "movie"
                     :country   (or country "US")
                     :attribute attribute
                     :term      term}
        form-query  (codec/form-encode query)]
    (str itunes-root "?" form-query)))

(defn query-itunes! [director country]
  (try
    (let [url      (build-itunes-api-search-url country "directorTerm" director)
          response (http/get url {:as :json})
          results  (->> response :body :results)]
      results)
    (catch Exception e [])))

(defn strip-movie-result-title [movie-string]
  "removes text within parentheses from movie string"
  (string/replace movie-string #"\([^)]*\)" ""))


(defn movie-title-contains-word [movie target]
  (if (utils/not-nil? target)
    (try
      (let [lower-case-target (string/lower-case target)]
        (let [match? (->> movie
                          :trackName
                          string/lower-case
                          (#(.contains % lower-case-target))
                          )]
          (if match? movie nil)))
      (catch Exception e nil))
    nil))

(defn movie-title-equals [movie target]
    (if (utils/not-nil? target)
      (try
       (let [lower-case-target (string/lower-case target)
             match? (->> movie
                          :trackName
                          string/lower-case
                          (#(= % lower-case-target))
                          )]
          (if match? movie nil))
       (catch Exception e nil))
      nil))

;; (defn get-diff-scores-for-movies [movie-results target-title]
;;   "generates similarity scores for each movie result with the target movie title
;;    (using levenshtein distances)"
;;   (->> movie-results
;;        (map :trackName)
;;        (map strip-movie-result-title)
;;        (map (partial diff/levenshtein-distance target-title))))

;; (defn find-index-of-best-result [diff-scores threshold]
;;   "returns vector index of best result,
;;    or -1 if threshold isn't met"
;;   (if (< 0 (count diff-scores))
;;     (let [indexed-vector-of-scores (map-indexed vector diff-scores)
;;           [best-idx best-score]    (apply min-key second indexed-vector-of-scores)]
;;       (if (<= best-score threshold)
;;         best-idx
;;         -1))
;;     -1))

;; ;;;;;;;;;;;;;;;;;;;;;;;
;; (defn get-smallest-difference-match [movie-results movie-title]
;;   (let [diff-scores     (get-diff-scores-for-movies movie-results movie-title)
;;         best-result-idx (find-index-of-best-result diff-scores 10)]
;;     (if (< 0 best-result-idx)
;;       (nth movie-results best-result-idx)
;;       nil)))


;; TODO: try with core-match!!

(defn get-best-match [candidate original-title english-title]
  "applies a list of matching predicates and returns the first result"
  ((some-fn #(movie-title-equals % english-title)
            #(movie-title-equals % original-title)
            #(movie-title-contains-word % english-title)
            #(movie-title-contains-word % original-title)
            #(movie-title-contains-word % (utils/get-longest-word english-title))
            #(movie-title-contains-word % (utils/get-longest-word original-title)))
   candidate))

(defn match-movie-with-itunes [{:keys [director original-title english-title country]}]
      (let [movie-results (query-itunes! director country)
            match         (first (filter #(get-best-match % original-title english-title) movie-results))]
        match))

(defn get-cached-itunes-result [{:keys [director originalTitle englishTitle] :as movie} country]
  (let [args     {:director       director
                  :original-title originalTitle
                  :english-title  englishTitle
                  :country        country}
        hash-key (str director originalTitle englishTitle)]
    (cache/cache-wrapper match-movie-with-itunes
                         args
                         hash-key)))


(defn append-metadata-to-film [movie country]
  (let [itunes-data            (get-cached-itunes-result movie country)
        affiliated-itunes-link (append-affiliate-code-to-link (:trackViewUrl itunes-data))]
    (if (not (empty? itunes-data))
      (assoc movie
             :image (:artworkUrl100 itunes-data)
             :itunes-url affiliated-itunes-link)
      movie)))

