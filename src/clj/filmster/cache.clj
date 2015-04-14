(ns filmster.cache
  (:require [filmster.dev :refer [is-dev?]]
            [taoensso.carmine :as carmine :refer (wcar)]))


(if is-dev?
  (def redis-spec nil)
  (def redis-spec {:host "gar.redistogo.com"
                   :port 9553
                   :user "redistogo"
                   :password "270223350046ca0978ecfe713525894e"}))

(def server1-conn {:pool {} :spec redis-spec})

(defmacro wcar* [& body] `(carmine/wcar server1-conn ~@body))

(defn flush-all []
  (wcar* (carmine/flushall)))

(defn set-key [key val]
  (wcar* (carmine/set key val)))

(defn get-key [key]
  (let [result (wcar* (carmine/get key))]
    result))

(defn cache-wrapper [fn args key]
  "returns value at key, or computes fn and stores result at key
   NOTE: stores empty results as empty map instead of nil, in order to cache them too"
  (let [cache-value (get-key key)]
    (if cache-value
      cache-value
      (let [result (fn args)]
        (set-key key (or result {}))
        result))))
