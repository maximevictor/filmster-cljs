(ns filmster.query-test
  (:require [clojure.test :refer :all]
            [filmster.itunes :as itunes]))

(deftest awards-smoke-test
  (let [q (itunes/query "almodovar")]
    (testing "Wrong number of awards"
      (prn (map :trackName q))
      (is true))))
