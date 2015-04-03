(ns filmster.fetching
  (:require [ajax.core :refer [GET POST json-request-format]]))

(defn fetch-resource [resource cb]
  (let [endpoint (str "/" resource "/")
        key      (-> resource name keyword)]
    (GET endpoint {:handler         cb
                   :response-format :json
                   :keywords?       true})))

(defn fetch-years [app-state]
  (GET "/years/" {:handler         #(swap! app-state assoc-in [:years] %)
                  :response-format :json
                  :keywords?       true}))

(defn fetch-movies [params app-state]
  (swap! app-state assoc-in [:fetching] true)
  (GET "/movies/" {:params          params
                   :handler         #(do (swap! app-state assoc-in [:results] %)
                                         (swap! app-state assoc-in [:fetching] false))
                   :response-format :json
                   :keywords?       true}))

(defn fetch-movie-details [movie app-state]
  (let [{:keys [englishTitle originalTitle director]} movie
        title (or englishTitle originalTitle)]
    (GET "/movie-details/" {:params {:movie-title title
                                     :director director}
                            :handler #(swap! app-state assoc-in [:featured :itunes-object] %)
                            :response-format :json
                            :keywords?       true})))
