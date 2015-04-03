(ns filmster.query-test
  (:require [clojure.test :refer :all]
            [filmster.query :as query]
            [filmster.utils :as utils]))

(deftest awards-smoke-test
  (let [a (query/get-awards)]
    (testing "Wrong number of awards"
      (is (= (count a) 18)))))

(deftest festivals-smoke-test
  (let [a (query/get-events)]
    (testing "Wrong number of festivals"
      (is (= (count a) 7)))))

(deftest years-smoke-test
  (let [y (query/get-years-bounds)]
    (testing "Wrong year interval"
      (is (= y [100 2015])))))

(deftest unfiltered-results-test
  (let [r (query/query-films)]
    (testing "Wrong number of first page results"
      (is (= (count r) 50)))))

(deftest filter-by-event-test
  (let [event (first (query/get-events))
        r     (query/query-films {:event      event
                               :year-start "2011"
                               :year-end   "2012"
                               })]
    (testing "Cannot filter films by event"
      (and
       (is (< 0 (count r)))
       (is (= event (first (map :event r))))))))

(deftest filter-by-award-test
  (let [event "Berlin"
        award (first (query/get-awards))
        r     (query/query-films {:event      event
                                  :award      award
                                  :year-start "100"
                                  :year-end   "2015"
                               })]
    (testing "Cannot filter films by award"
      (and
       (is (< 0 (count r)))
       (is (= award (first (map :award r))))))))


(deftest filter-by-year-test
  (let [start-date 1990
        end-date   2010
        event      "Berlin"
        r          (query/query-films {:event      event
                                    :year-start (str start-date)
                                    :year-end   (str end-date)
                                    })
       years      (distinct (map #(-> % :year name utils/parse-int) r))]

    (testing "Cannot filter films by year"
      (is (every? #(<= start-date % end-date)
                  years)))))
