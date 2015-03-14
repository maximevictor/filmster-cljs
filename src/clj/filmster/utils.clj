(ns filmster.utils)

(defn parse-int [s]
  (let [match (re-find #"\d+" s)]
    (if (not (nil? match))
      (Integer. match))))
