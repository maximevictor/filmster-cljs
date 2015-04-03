(ns filmster.data
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

(defn append-data-to-movie [films year event]
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
       (map #(append-data-to-movie (% data) % event))
       flatten))

(def film-data (->> festivals
                    (map #(flatten-festival-list (get-festival-data %) %))
                    flatten))

