(ns filmster.itunes
  (:require [clj-http.client :as http]
            [clojure.data.json :as json]))

(defn construct-itunes-search [term country attribute]
  (let [itunes-root  "https://itunes.apple.com/search"
        query-params "&media=movie&entity=movie&country="
        attribute-params (str "&attribute=" (or attribute "directorTerm"))]
    (str itunes-root "?term=" term query-params country attribute-params)))

(defn query [term]
  (let [response (http/get (construct-itunes-search term "US" nil)
                           {:as :json})
        results  (->> response :body :results)]
    results))

;; artistName
;; artworkUrl100
;; artworkUrl30
;; artworkUrl60
;; collectionExplicitness
;; collectionHdPrice
;; collectionPrice
;; contentAdvisoryRating
;; country
;; currency
;; kind
;; longDescription
;; previewUrl
;; primaryGenreName
;; radioStationUrl
;; releaseDate
;; shortDescription
;; trackCensoredName
;; trackExplicitness
;; trackHdPrice
;; trackId
;; trackName
;; trackPrice
;; trackTimeMillis
;; trackViewUrl
;; wrapperType
