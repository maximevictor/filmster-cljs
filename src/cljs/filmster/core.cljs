(ns filmster.core
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [reagent.core :as reagent :refer [atom]]
            [cljs.core.async :as async :refer [put! chan alts! >!]]
            [shodan.console :as console :include-macros true]
            [clojure.string :as string]

            [filmster.fetching :as fetching]
            [filmster.components.movie :as movie]
            [filmster.utils :as utils])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def HOLDING-TEXT "Filmster is a smart way to discover the best movies on iTunes, in competition at top international Film Festivals and Award Ceremonies. It also includes annual top 10 lists from the celebrated French magazine 'Cahiers du Cinéma.' Never waste your time looking for a good film again!")

(defn jlog [v]
  (.log js/console (clj->js v)))

(enable-console-print!)

(defonce app-state (atom {
                          :events    []
                          :awards    []
                          :countries []
                          :years     [1980 2015]
                          :query     {:event      #{}
                                      :award      #{}
                                      :year-start 2000
                                      :year-end   2015
                                      :country    {:name "USA" :countryCode "US"}}
                          :results   []
                          :featured  nil
                          :fetching  false
                          :filters   {:available true}
                          }))

(def dom-entry-point (. js/document (getElementById "app")))

(defn init []
  (fetching/fetch-resource "events"
                           (fn [results]
                             (let [result-set (into #{} results)]
                               (swap! app-state assoc-in [:events] results)
                               (swap! app-state assoc-in [:query :event] result-set))))
  (fetching/fetch-resource "awards"
                           (fn [r]
                             (swap! app-state assoc-in [:awards] r)))
  (fetching/fetch-resource "countries"
                           (fn [r]
                             (swap! app-state assoc-in [:countries] r)))
  ;; (add-watch app-state :query-year-start-watch
  ;;            (fn [_ atom _ new-state]
  ;;              (let [{:keys [year-start year-end]} (:query new-state)]
  ;;                (if (<= year-end year-start)
  ;;                  (swap! atom assoc-in [:query :year-end] year-start)))) )
  )

(defn proportion-selected [{:keys [selected total]}]
  (let [sel-count   (count selected)
        total-count (count total)]
    [:span (if (= sel-count total-count)
             [:span " (All)"]
             [:span (str " (" sel-count "/" total-count ")")])]
    ))


(defn collapsible-entry [title icon-class badge component]
  [:li [:div.collapsible-header
        [:div [:i {:class icon-class}]
         [:span title badge]
         [:span.badge [:i {:class "mdi-navigation-arrow-drop-down"}]]]
        ]
   [:div.collapsible-body component]
   ])

(defn switch-for-set-entry [{:keys [value field-set key]}]
  ;; value = single, e.g. "Berlin"
  ;; field-set = #{set} in which to place value if toggled
  (let [on (contains? field-set value)]
    [:div.switch
     [:label
      [:span.switch__label value]
      [:input {:value value
               :checked on
               :onChange #(swap! app-state assoc-in [:query key]
                                 (if on
                                   (disj field-set value)
                                   (conj field-set value)))
               :type "checkbox"}
       [:span.lever]]
      ]]))

(defn switch-input [key label]
  (let [value (-> @app-state :filters key)]
        [:div.switch
         [:label
          [:span.switch__label label]
          [:input {:value value
                   :checked value
                   :onChange #(swap! app-state assoc-in [:filters key] (not value))
                   :type "checkbox"}
           [:span.lever]]
          ]]))

(defn slider [{:keys [label field-name owner interval]}]
  (let [[min max] interval
        key       (-> field-name name keyword)
        value     (-> @app-state :query key)]
    [:div.row
     [:div.col.s9
      ;; [:label label]
      [:input {:type "range" :value value
               :name field-name :min min :max max
               :onChange (fn [el] (let [val (-> el .-target .-value)]
                                    (swap! app-state assoc-in [:query key] val)))}]]
     [:div.col.s3 value]]))

(defn interval-selected []
  (let [start (-> @app-state :query :year-start)
        end   (-> @app-state :query :year-end)]
    [:span (str " (" start " - " end ")")])
  )

(defn submit-fn [e]
  (swap! app-state assoc-in [:results] {})
  (let [query-map (:query @app-state)]
    (fetching/fetch-movies query-map app-state))
  (.preventDefault e))

(defn search-form []
  [:ul.collapsible {:data-collapsible "accordion"}

   [collapsible-entry "Events" "mdi-av-movie"
    [proportion-selected {:selected (-> @app-state :query :event)
                          :total    (-> @app-state :events)}]
    (for [event (-> @app-state :events)]
      (switch-for-set-entry {:value     event
                             :field-set (-> @app-state :query :event)
                             :key       :event}))
    ]

   [collapsible-entry "Awards" "mdi-action-grade"
    [proportion-selected {:selected (-> @app-state :query :award)
                          :total (-> @app-state :awards)}]
    (for [award (-> @app-state :awards)]
      (switch-for-set-entry {:value     award
                             :field-set (-> @app-state :query :award)
                             :key       :award}))
    ]

   [collapsible-entry "Years" "mdi-action-event"
    [interval-selected]
    [:div
     [slider {:label      "Start year"
              :field-name "year-start"
              :owner      (-> @app-state :query)
              :interval   (-> @app-state :years)}]
     [slider {:label      "End year"
              :field-name "end-start"
              :owner      (-> @app-state :query)
              :interval   (-> @app-state :years)}]]]

   [:li [:div.collapsible-header [:i.mdi-content-flag]
         "App store: " (-> @app-state :query :country :name)]]
   [:li [:div.collapsible-header [:button
                                  {:onClick submit-fn
                                   :class "btn waves-effect"}
                                  "Search"]]]
   ])

(defn side-panel []
  [:div
   [:nav [:ul#slide-out.side-nav.fixed
          [:h3 "Filmster"]
          [search-form]]
    [:a.button-collapse {:href "#"
                         :data-activates "slide-out"}
     [:i.mdi-navigation-menu]]]])

(defn side-panel-did-mount []
  (js/$ (fn []
          (let [opts {:menuWidth 320}]
            (.sideNav (js/$ ".button-collapse")
                      (clj->js opts))
            (.collapsible (js/$ ".collapsible"))))))

(defn side-panel-component []
  (reagent/create-class {:reagent-render      side-panel
                         :component-did-mount side-panel-did-mount}))

(defn movie-is-available [movie]
  (contains? movie :image))

(defn fetching-indicator [_]
  [:div.progress
   [:div.indeterminate]])

(defn results-header [{:keys [only-show-available results filtered-results]}]
  [:div
   [:h2 "Results"]
   [:h4 (if only-show-available
           (str " (" (count filtered-results) "/" (count results) ")")
           (str " (" (count results) ")"))]])


(defn holding-screen []
  [:div [:h2 [:img {:src "/img/favicon.png"} "Filmster"]]
   [:p HOLDING-TEXT]]
  )

(defn results []
  (let [only-show-available (-> @app-state :filters :available)
        results             (-> @app-state :results)
        has-results         (< 0 (count results))
        filtered-results    (filter movie-is-available results)
        results-to-show     (if only-show-available filtered-results results)]
    [:div.col.m6.s12

     (if has-results
       [:div [results-header {:only-show-available only-show-available
                              :results             results
                              :filtered-results    filtered-results}]
        (switch-input :available "Show only available")
        (for [movie results-to-show]
          [movie/movie-card movie])
        ]
       [holding-screen])
     (if (:fetching @app-state)
       [fetching-indicator])


     ]
    ))

(defn app []
  [:div
   [:div
    [side-panel-component]]
   [:div#main
    [:div.container
     [:div.row
      [results]]]]])

(defn main []
  (init)
  (reagent/render [app]
                  (js/document.getElementById "app")))
