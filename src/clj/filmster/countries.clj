(ns filmster.countries
  (:require [clojure.data.json :as json]))

(def countries (json/read-str (slurp "resources/CountryCodes.json")
                            :key-fn keyword))

(keys (first countries))
;; (:name :countryCode)

(map :name countries)
