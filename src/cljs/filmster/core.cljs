(ns filmster.core
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            ;; [secretary.core :as secretary :refer-macros [defroute]]
            [cljs.core.async :as async :refer [put! chan alts! >!]]
            [shodan.console :as console :include-macros true]
            [clojure.string :as string]

            [filmster.fetching :as fetching]
            [filmster.utils :as utils]
            [filmster.components.search :as search]
            [filmster.components.movies :as movies])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(enable-console-print!)

(def jquery (js* "jQuery"))

(def AFFILIATE-LINK "at=1l3vvZJ")

(defonce app-state (atom {
                          :events           []
                          :awards           []
                          :years            [1980 2015]
                          :query            {:event     #{}
                                             :award     #{}
                                             :year-start 2000
                                             :year-end   2015}

                          :results          []
                          :featured         nil
                          :fetching         false
                          :available-filter true

                          :submit-fn        (fn [e]
                                              (swap! app-state assoc-in [results] {})
                                              (let [query-map (:query @app-state)]
                                                (fetching/fetch-movies query-map app-state)))
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
  (add-watch app-state :query-year-start-watch
             (fn [_ atom _ new-state]
               (let [{:keys [year-start year-end]} (:query new-state)]
                 (if (<= year-end year-start)
                   (swap! atom assoc-in [:query :year-end] year-start)))) ))

(defcomponent header []
  (render [_]
          (dom/nav
           (dom/div {:class "nav-wrapper"}
                    (dom/a {:class "brand-logo"} "Filmster")
                    (dom/ul {:id "nav-mobile"
                             :class "right hide-on-med-and-down"}
                            (dom/li (dom/a {:href "#"} "About")))))))


(defcomponent nav []
  (render [_]
          (dom/nav
           (dom/ul {:id "slide-out"
                    :class "side-nav fixed"}
                    (dom/li (dom/a {:href "#"} "First"))
                    (dom/li (dom/a {:href "#"} "First")))
           (dom/a {:data-activates "slide-out"
                   :class "button-collapse"}
                  (dom/i {:class "mdi-navigation-menu"})))))

(defcomponent featured-result [{:keys [featured] :as data} owner]
  (did-mount [_]
            (-> (jquery "#movie-detail") (.openModal)))

  (render [_]
          (let [image-url         (-> featured :itunes-object :artworkUrl100)
                itunes-link       (-> featured :itunes-object :trackViewUrl)
                no-protocol-image (second (.split image-url ":"))]
            (dom/div {:id "movie-detail"
                      :class "modal"}
                     (dom/div {:class "modal-content"}
                              (dom/h4 {:style {:color "black"}}
                                      (str (:englishTitle featured)
                                           " - "
                                           (:originalTitle featured)))
                              (dom/p (:description featured)))
                     (dom/img {:src no-protocol-image})
                     (dom/div {:class "modal-footer"}
                              (if itunes-link
                                (dom/a {:href    itunes-link
                                        :target   "_"}
                                       "See in itunes"))
                              (dom/a {:class    "modal-action modal-close waves-effect waves-green btn-flat"
                                      :on-click #(swap! app-state assoc :featured nil)}
                                     "X"))))))

(defcomponent app [data owner]
  (init-state [_]
              {:feature (chan)})

  (will-mount [_]
              (let [movie-clicked (om/get-state owner :feature)]
                (go (loop []
                      (let [featured-movie (<! movie-clicked)]
                        (fetching/fetch-movie-details featured-movie app-state)
                        (om/transact! data :featured (fn [e] featured-movie))
                      (recur))))))

  (render-state [_ {:keys [feature]}]
   (dom/div (search/->movie-form data)
            (dom/div {:id "main"}
             (->header data)
             (dom/div {:class "container"}
                      (dom/div {:class "row"}
                               (if (utils/not-nil? (:featured data))
                                 (->featured-result data))
                               (search/->movie-results data {:init-state {:feature feature}})
                               ))))))

(defn main []
  (init)
  (om/root
   app
   app-state
   {:target dom-entry-point}))
