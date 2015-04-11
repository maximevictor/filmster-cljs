(ns filmster.redis
  (:require [filmster.dev :refer [is-dev?]]
            [taoensso.carmine :as carmine :refer (wcar)]))


(if is-dev?
  (def redis-port 6379)
  (def redis-port 9553) ;; redis-to-go heroku
  )

(def server1-conn {:pool {} :spec {:port redis-port}})

(defmacro wcar* [& body] `(carmine/wcar server1-conn ~@body))

(defn flush-all []
  (wcar* (carmine/flushall)))

(defn set-key [key val]
  (wcar* (carmine/set key val)))

(defn get-key [key]
  (let [result (wcar* (carmine/get key))]
    result))
