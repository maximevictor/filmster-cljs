(ns filmster.redis
  (:require [taoensso.carmine :as carmine :refer (wcar)]))

(def server1-conn {:pool {} :spec {}})

(defmacro wcar* [& body] `(carmine/wcar server1-conn ~@body))

(defn set-key [key val]
  (wcar* (carmine/set key val)))

(defn get-key [key]
  (let [result (wcar* (carmine/get key))]
    result))
