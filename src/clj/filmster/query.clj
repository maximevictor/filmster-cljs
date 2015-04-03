(ns filmster.query
  (:require [clojure.data.json :as json]
            [clojure.string :as str]
            [filmster.utils :as utils]
            [filmster.data :as data]))

(defn build-filter-set [key params]
  "builds #{set} of values of type 'key' to combine in query"
  (let [filter-list   (key params)
        filter-vector (utils/enforce-vector filter-list)
        filter-set    (set filter-vector)]
    filter-set))

(defn build-query [kw params]
  "returns filter predicate"
  (let [filter-set (build-filter-set kw params)]
    (fn [entry] (or (empty? filter-set)
                    (contains? filter-set (kw entry))))))

(defn build-year-set [params]
  (let [start    (-> params :year-start utils/parse-int)
        end      (-> params :year-end utils/parse-int)
        interval (range start
                        (+ 1 end))]
    (->> interval
         (map (comp keyword str))
         set)))

(defn query-film-data [params]
  (let [year-set (build-year-set params)
        predicates (every-pred (fn [film] (contains? year-set (:year film)))
                               (build-query :event params)
                               (build-query :award params))
        results  (filter predicates data/film-data)]
    (sort-by :year results)))

(defn query-films
  ([] (take 50 data/film-data))
  ([params] (query-film-data params)))

(defn get-awards []
  (->> data/film-data
       (map :award)
       (remove nil?)
       (map #(str/split % #", "))
       flatten
       distinct))

(defn get-events []
  (->> data/film-data
       (map :event)
       distinct))

(defn get-years []
  (->> data/film-data
       (map :year)
       distinct
       (map name)
       (map utils/parse-int)
       (filter number?)))

(defn get-years-bounds []
  (let [all-years (get-years)
        min       (apply min all-years)
        max       (apply max all-years)]
    [min max]))
