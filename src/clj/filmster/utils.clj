(ns filmster.utils
  (:require [clojure.string :as string]))

(defn parse-int [s]
  (let [match (re-find #"\d+" s)]
    (if (not (nil? match))
      (Integer. match))))

(defn enforce-vector [v]
  (if (vector? v) v [v]))

(defn get-longest-word [title]
  (if title
    (let [words-in-title        (string/split title #" ")
          longest-word-in-title (apply max-key count words-in-title)]
      longest-word-in-title)))

(def not-nil? (complement nil?))
