(ns filmster.itunes
  (:require [clj-http.client :as http]
            [clj-diff.core :as diff]
            [clojure.data.json :as json]))

(defn construct-itunes-search [director country attribute]
  (let [itunes-root  "https://itunes.apple.com/search"
        query-params "&media=movie&entity=movie&country="
        attribute-params (str "&attribute=" (or attribute "directorTerm"))]
    (str itunes-root "?term=" director query-params country attribute-params)))

;; "https://itunes.apple.com/search?term=%@&media=movie&entity=movie&attribute=directorTerm&country=%@", uri-encode(directorName) countryCode

(defn query-itunes [director]
  (let [response (http/get (construct-itunes-search director "US" nil)
                           {:as :json})
        results  (->> response :body :results)]
    results))

(defn get-closest-itunes-result [director movie-title]
  (let [results            (query-itunes director)
        candidates         (map :trackName results)
        application        (partial diff/levenshtein-distance movie-title)
        leverstein-scores  (map application candidates)
        leverstein-kv      (map-indexed vector leverstein-scores)
        best-candidate-idx (first (apply min-key second leverstein-kv))
        best-candidate     (nth results best-candidate-idx)
        ]
    best-candidate))


;; [{:trackName "The Trip",
;;   :artworkUrl60 "http://is3.mzstatic.com/image/pf/us/r30/Video/9b/df/f5/mzi.lxrzufkh.60x60-50.jpg",
;;   :radioStationUrl "https://itunes.apple.com/station/idra.461694942",
;;   :trackPrice 9.99,
;;   :collectionHdPrice 12.99,
;;   :collectionPrice 9.99,
;;   :artistName "Michael Winterbottom",
;;   :primaryGenreName "Comedy",
;;   :trackTimeMillis 6704766,
;;   :releaseDate "2011-04-24T07:00:00Z",
;;   :trackId 461694942,
;;   :previewUrl "http://a1220.v.phobos.apple.com/us/r1000/068/Video/20/53/82/mzm.idonhfgs..640x460.h264lc.d2.p.m4v",
;;   :artworkUrl30 "http://is4.mzstatic.com/image/pf/us/r30/Video/9b/df/f5/mzi.lxrzufkh.30x30-50.jpg",
;;   :currency "USD",
;;   :trackViewUrl "https://itunes.apple.com/us/movie/the-trip/id461694942?uo=4",
;;   :trackHdRentalPrice 4.99,
;;   :wrapperType "track",
;;   :trackRentalPrice 3.99,
;;   :kind "feature-movie",
;;   :collectionExplicitness "notExplicit",
;;   :trackExplicitness "notExplicit",
;;   :trackCensoredName "The Trip",
;;   :longDescription "THE TRIP is an improvised tour of the North of England reuniting comedy favorites Steve Coogan and Rob Brydon. In the style of Curb your Enthusiasm, the story is fictional but based around their real personas. When Steve is commissioned by the food supplement of a Sunday newspaper to review half a dozen restaurants, he decides to mix work with pleasure and plans a trip around the North of England with his food loving American girlfriend. But when his girlfriend decides to leave him and return to the States, Steve is faced with a week of meals for one, not quite the trip he had in mind. Reluctantly, he calls Rob, the only person he can think of who will be available. Rob, never one to turn down a free lunch (let alone six) agrees, and together they set off for a culinary adventure.",
;;   :artworkUrl100 "http://is1.mzstatic.com/image/pf/us/r30/Video/9b/df/f5/mzi.lxrzufkh.100x100-75.jpg",
;;   :trackHdPrice 12.99,
;;   :contentAdvisoryRating "Unrated",
;;   :country "USA"}]
