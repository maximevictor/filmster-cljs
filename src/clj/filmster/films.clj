(ns filmster.films
  (:require [clojure.data.json :as json]
            [clojure.string :as str]
            [filmster.utils :as utils]))

(def festivals ["Berlin"
                "CahiersDuCinema"
                "Cannes"
                "Cesar"
                "Oscars"
                "SightSound2012"
                "Venice"])

;; "Directors"

(defn append-extra-rows-to-movies [films year event]
  (map #(assoc % :year year
               :event event) films))

(defn get-festival-data [festival]
  (let [data-file (str "resources/" festival ".json")
        file      (slurp data-file)
        data      (json/read-str file :key-fn keyword)]
    data))

(defn flatten-festival-list [data event]
  "embed year and event data into film list.

   {:2015 [{:originalTitle '45 Years', :director 'Andrew Haigh', :award 'Best Actress, Best Actor' ...}]}
   => {:event 'Berlin', :year :2015, :originalTitle '45 Years', :director 'Andrew Haigh', :award 'Best Actress, Best Actor' ...}

   assumes films are nested by year"
  (->> data
       keys
       (map #(append-extra-rows-to-movies (% data) % event))
       flatten))

(def film-data (->> festivals
                    (map #(flatten-festival-list (get-festival-data %) %))
                    flatten))

(defn enforce-vector [v]
  (if (vector? v) v [v]))

(defn build-filter-set [kw params]
  "builds #{set} of values of type 'kw' to combine in query"
  (let [filter-list   (kw params)
        filter-vector (enforce-vector filter-list)
        filter-set    (set filter-vector)]
    filter-set))

(defn build-query [kw params]
  "returns filter predicate"
  (let [filter-set (build-filter-set kw params)]
    (fn [entry] (contains? filter-set (kw entry)))))

(defn build-year-interval-set [params]
  (let [start    (-> params :year-start utils/parse-int)
        end      (-> params :year-end utils/parse-int)
        interval (range start (+ 1 end))]
    (set (map (comp keyword str) interval))))

(defn filter-films [params]
  (print params)
  (let [year-set (build-year-interval-set params)]
    (filter (and (build-query :event params)
                 (build-query :award params)
                 (fn [entry] (contains? year-set (:year entry))))
            film-data)))

(defn get-data
  ([] (take 50 film-data))
  ([params] (filter-films params)))

(defn get-awards []
  (->> film-data
       (map :award)
       (remove nil?)
       (map #(str/split % #", "))
       flatten
       distinct))

(defn get-festivals []
  (->> film-data
       (map :event)
       distinct))

(defn get-years []
  (->> film-data
       (map :year)
       distinct
       (map name)
       (map utils/parse-int)
       (filter number?)))

(defn get-years-bounds []
  (let [all-years (get-years)
        min (apply min all-years)
        max (apply max all-years)]
    [min max]))
