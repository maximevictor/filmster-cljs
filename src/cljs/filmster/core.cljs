(ns filmster.core
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [ajax.core :refer [GET POST json-request-format]]
            [cljs.core.async :refer [<!]]
            form.juice
            ;; [goog.dom.json :as gjson]
            [domina :refer [by-id]]
            [shodan.console :as console :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(enable-console-print!)

(def not-nil? (complement nil?))

(defonce app-state (atom {:title "Filmster"
                          :results []
                          :festivals []
                          :awards []
                          :year-interval [1980 2014]
                          :year-start nil
                          :fetching false
                          :query {}}))

(def dom-entry-point (. js/document (getElementById "app")))


(defn fetch-awards []
  (GET "/awards/" {:handler #(swap! app-state assoc-in [:awards] %)
                   :response-format :json
                   :keywords? true}))

(defn fetch-festivals []
  (GET "/festivals/" {:handler #(swap! app-state assoc-in [:festivals] %)
                      :response-format :json
                      :keywords? true}))

(defn fetch-years []
  (GET "/years/" {:handler #(swap! app-state assoc-in [:year-interval] %)
                  :response-format :json
                  :keywords? true}))

(defn fetch-movies [params]
  (swap! app-state assoc-in [:fetching] true)
  (GET "/movies/" {:params params
                   :handler #(do (swap! app-state assoc-in [:results] %)
                                 (swap! app-state assoc-in [:fetching] false)
                                 )
                   :response-format :json
                   :keywords? true}))

(defn switch-component [label name]
  (dom/div {:class "switch"}
           (dom/label
            (dom/span {:class "switch__label"} label)
            (dom/input {:name name
                        :value label
                        :type "checkbox"}
                       (dom/span {:class "lever"}))
            )))

(defcomponent festival-entry [festival]
  (render [_] (switch-component festival "event")))

(defcomponent award-entry [award]
  (render [_] (switch-component award "award")))

(defcomponent festival-picker [{:keys [festivals]} owner]
  (render [_]
          (dom/div {:class "festival-picker"}
                   (dom/h3 "Festival")
                   (for [f festivals]
                     (->festival-entry f)))))

(defcomponent award-picker [{:keys [awards query]} owner]
  (render [_]
          (dom/div {:class "award-picker"}
                   (dom/h3 "Award")
                   (for [f awards]
                     (->award-entry f query)))))

(defcomponent year-interval-picker [{:keys [label name year-interval year-start]} owner]
  (render [_]
          (let [start (first year-interval)
                end (last year-interval)]
            (dom/div
             (dom/label label)
             (dom/span start)
             (dom/input {:type "range"
                         :name name
                         :min (first year-interval)
                         :max (last year-interval)})
             (dom/span end)))))

(defn submit-query [e]
  (let [form-map (form.juice/squeeze e)]
    (console/log (clj->js form-map))
    (fetch-movies form-map)
    (.preventDefault e)))

(defcomponent search-btn [owner]
  (render [_]
          (dom/button {:onClick submit-query
                       :class "btn waves-effect"}
                      "Search")))

(defcomponent movie-form [owner]
  (render [_]
          (dom/div {:class "col s6"}
                   (dom/form {:id "query-form"}
                             (dom/h2 "Form")
                             (->search-btn owner)
                             (->festival-picker owner)
                             (->award-picker owner)
                             (->year-interval-picker (assoc owner :name "year-start"
                                                            :label "Start year"
                                                            ))
                             (->year-interval-picker (assoc owner :name "year-end"
                                                            :label "End year"
                                                            ))
                             (->search-btn owner)))))

(defcomponent movie [{:keys [originalTitle
                             englishTitle
                             year
                             director
                             country
                             award]} owner]
  (render [_]
          (dom/div {:class "movie-result-card card"}
                   (dom/div {:class "card-content"}
                            (dom/div {:class "card-title"}
                                     originalTitle)
                            (if (not-nil? englishTitle)
                              (dom/span (str " (" englishTitle ") ")))
                            (if (not-nil? director)
                              (dom/div  director))
                            (dom/span year)
                            (dom/div award))
                            )))

(defcomponent fetching-indicator [owner]
  (render [_]
          (dom/div {:class "progress"}
                   (dom/div {:class "indeterminate"}))))

(defcomponent movie-results [{:keys [results fetching]} owner]
  (render [_]
          (dom/div {:class "col s6"}
                   (dom/h2
                    (dom/span "Results")
                    (dom/span (str " (" (count results) ")")))
                   (if fetching
                     (->fetching-indicator owner)
                     (for [movie results]
                       (->movie movie))))))

;; award: "Best Actress, Best Actor"
;; country: "United Kingdom"
;; director: "Andrew Haigh"
;; englishTitle: "45 Years"
;; event: "Berlin"
;; originalTitle: "45 Years"
;; year: "2015"


(defcomponent header [{:keys [title]} owner]
  (render [_]
          (dom/nav
           (dom/div {:class "nav-wrapper"}
                    (dom/a {:class "brand-logo"} "Filmster")
                    (dom/ul {:id "nav-mobile"
                             :class "right hide-on-med-and-down"}
                            (dom/li (dom/a {:href "#"} "About")))))))

(defcomponent app [owner]
  (render [_]
          (dom/div
           (->header owner)
           (dom/div {:class "container"}
                    (dom/div {:class "row"}
                             (->movie-form owner)
                             (->movie-results owner)
                             )))))


(defn main []
  (om/root
   app
   app-state
   {:target dom-entry-point}))

(fetch-festivals)
(fetch-awards)
;; (fetch-years)
