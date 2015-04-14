(ns filmster.query-test
  (:require [clojure.test :refer :all]
            [filmster.itunes :as itunes]
            [filmster.data :as data]))

(def OFFLINE false)

(if (not OFFLINE)

  (deftest query-itunes-test
    (let [q (itunes/query-itunes! "almodovar" "US")]
      (is (= 9 (count q))))))

(deftest movie-title-equals-test
  (let [m {:trackName "The White Ribbon"
           :otherKeys nil}]
    (is (= m (itunes/movie-title-equals m "the white ribbon")))))

(deftest movie-title-contains-test
  (let [m {:trackName "The White Ribbon"
           :otherKeys nil}]
    (is (= m (itunes/movie-title-contains-word m "white")))
    (is (= m (itunes/movie-title-contains-word m "ribbon")))
    (is (= m (itunes/movie-title-contains-word m "white ribbon")))))

(deftest get-best-match-test
  (let [m              {:trackName "The White Ribbon"
                        :otherKeys nil}
        original-title "das weisse band"
        english-title  "the white ribbon"
        ]
    (is (= m (itunes/get-best-match m original-title english-title)))))

(deftest get-best-match-from-local-list-test
  (let [candidates     [{:trackName "Jimmy's Hall"}
                        {:trackName "Abdisalam"}
                        {:trackName "The White Ribbon"}
                        {:trackName "Maps to the Starts"}]
        original-title "das weisse band"
        english-title  "the white ribbon"
        matches        (filter #(itunes/get-best-match % original-title english-title)
                               candidates)]
    (is (= "The White Ribbon" (-> matches first :trackName)))))

(if (not OFFLINE)

  (deftest get-best-match-from-itunes-test
    (let [query {:director "haneke"
                 :original-title "das weisse band"
                 :english-title "the white ribbon"
                 :country "US"}
          match (itunes/match-movie-with-itunes query)]
      (is (= "The White Ribbon" (-> match :trackName))))))

(deftest get-best-match-from-cache-test
  (let [query {:director "haneke"
               :originalTitle "das weisse band"
               :englishTitle "the white ribbon"}
        match (itunes/get-cached-itunes-result query "US")]
    (is (= "The White Ribbon" (-> match :trackName)))))

(deftest get-metadata-for-movie-test
  (let [query {:director "haneke"
               :originalTitle "das weisse band"
               :englishTitle "the white ribbon"}
        match (itunes/append-metadata-to-film query "US")]
    (is (.contains (:image match) "static"))
    (is (.contains (:itunes-url match) "itunes"))))


;; (defn test-a-few []
;;   (for [i (range 0 15)]
;;     (let [m (nth filmster.data/film-data i)]
;;       (match-movie-with-itunes {:director (m :director)
;;                                 :original-title (m :originalTitle)
;;                                 :english-title (m :englishTitle)
;;                                 :country "US"}))))
