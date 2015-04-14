(ns filmster.utils-test
  (:require [clojure.test :refer :all]
            [filmster.utils :as utils]))

(deftest longest-word-test
  (let [sentence "one two three four five seven eight verylongword ten"
        longest-word (utils/get-longest-word sentence)]
    (is (= longest-word "verylongword"))))
