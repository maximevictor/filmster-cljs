(ns filmster.components.search
  (:require [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [clojure.set :refer [difference union]]
            [om.core :as om :include-macros true]
            [cljs.core.async :as async :refer [put! chan alts! <! >!]]
            [filmster.fetching :as fetching]
            [filmster.components.movies :as movies]
            [filmster.utils :as utils])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(def jquery (js* "$"))

(defcomponent switch-input-into-set [{:keys [value field-set]}]
  ;; value = single, e.g. "Berlin"
  ;; field-set = #{set} in which to place value if toggled
  (render [_]
          (let [on (contains? field-set value)]
            (dom/div {:class "switch"}
                     (dom/label
                      (dom/span {:class "switch__label"} value)
                      (dom/input {:value value
                                  :checked on
                                 :on-click (fn [_] (om/transact! field-set (fn [s]
                                                                              (if on (disj s value) (conj s value)))))
                                  :type "checkbox"}
                                 (dom/span {:class "lever"}))
                      )))))

(defcomponent switch-input [{:keys [filters key label]}]
  (render [_]
          (dom/div {:class "switch"}
                   (dom/label
                    (dom/span {:class "switch__label"} label)
                    (dom/input {:checked (:available filters)
                                :on-change (fn [_] (om/transact! filters key #(not %)))
                                :type "checkbox"}
                               (dom/span {:class "lever"}))
                    ))))

(defcomponent event-picker [{:keys [events query] :as data} owner]
  (render [_]
          (dom/div {:class "event-picker"}
                   (for [event events]
                     (->switch-input-into-set {:value event
                                               :field-set (:event query)})))))

(defcomponent award-picker [{:keys [awards query] :as data} owner]
  (render [_]
          (dom/div {:class "award-picker"}
                   (for [award awards]
                     (->switch-input-into-set {:value award
                                               :field-set (:award query)})))))

(defcomponent year-picker [{:keys [label name years]} owner]
  (render [_]
          (let [start (first years)
                end   (last years)]
            (dom/div
             (dom/label label)
             (dom/span start)
             (dom/input {:type "range"
                         :name name
                         :min (first years)
                         :max (last years)})
             (dom/span end)))))

(defcomponent slider-input [{:keys [label field-name owner interval]}]
  (render [_]
          (let [[min max] interval
                key       (-> field-name name keyword)
                value     (key owner)]
            (dom/div {:class "row"}
                     (dom/div {:class "col s9"}
                              (dom/input {:type "range"
                                          :name field-name
                                          :min min
                                          :max max
                                          :value value
                                          :on-change (fn [el] (let [val (-> el .-target .-value)]
                                                                (om/update! owner key val)))}))
                     (dom/label {:class "col s3"} value)
                     ))))

(defcomponent search-btn [{:keys [submit-fn]} owner]
  (render [_]
          (dom/button {:onClick (fn [e] (submit-fn e) (.preventDefault e))
                       :class "btn waves-effect"}
                      "Search")))

(defn collapsible-entry [title icon-class badge component]
  (dom/li
   (dom/div {:class "collapsible-header"}
            (dom/div (dom/i {:class icon-class})
                     (dom/span title
                               badge)
                     (dom/span {:class "badge"}
                               (dom/i {:class "mdi-navigation-arrow-drop-down"}))))
   (dom/div {:class "collapsible-body"} component)))

(defcomponent set-count-display [{:keys [selected total]} owner]
  (render [_]
          (let [sel-count   (count selected)
                total-count (count total)]
            (dom/span (if (= sel-count total-count)
               (dom/span " (All)")
               (dom/span (str " (" sel-count "/" total-count ")"))))
            )))

(defcomponent interval-display [{:keys [year-start year-end]}]
  (render [_]
          (let [string (if (= year-start year-end)
                         year-start
                         (str year-start "-" year-end))]
                      (dom/span (str " (" string ")")))))

(defcomponent movie-form [owner]
  (did-mount [_]
             (-> (jquery ".button-collapse") (.sideNav))
             (-> (jquery ".collapsible") (.collapsible)))

  (render [_]
          (let [query     (:query owner)
                selected (:event query)
                total     (:events owner)]
            (dom/nav
             (dom/ul {:id "slide-out"
                      :class "side-nav fixed"}
                     (dom/form {:id "query-form"}
                               (->search-btn owner)
                               (dom/ul {:class "collapsible" :data-collapsible "accordion"}
                                       (collapsible-entry "Events" "mdi-av-movie"
                                                          (->set-count-display {:selected (:event query)
                                                                                :total (:events owner)})
                                                          (->event-picker owner))
                                       (collapsible-entry "Awards" "mdi-action-grade"
                                                          (->set-count-display {:selected (:award query)
                                                                                :total (:awards owner)})
                                                          (->award-picker owner))
                                       (collapsible-entry "Years" "mdi-action-event"
                                                          (->interval-display query)
                                                          (dom/div  (->slider-input {:label      "Start year"
                                                                                     :field-name "year-start"
                                                                                     :owner      query
                                                                                     :interval   (:years owner)})
                                                                    (->slider-input {:label      "End year"
                                                                                     :field-name "year-end"
                                                                                     :owner      query
                                                                                     :interval   (:years owner)}))))
                               ))
             (dom/a {:href "#"
                     :data-activates "slide-out"
                     :class "button-collapse"}
                    (dom/i {:class "mdi-navigation-menu"})
                    )))))

(defcomponent fetching-indicator [_]
  (render [_]
          (dom/div {:class "progress"}
                   (dom/div {:class "indeterminate"}))))

(defn movie-is-available [movie]
  (contains? movie :image))

(defcomponent movie-results [{:keys [results fetching filters] :as owner}]
  (render-state [_ {:keys [feature]}]
                (let [only-show-available (:available filters)
                      has-results         (< 0 (count results))
                      filtered-results    (filter movie-is-available results)
                      results-to-show     (if only-show-available filtered-results results)]
                  (dom/div {:class "col s6"}
                           (dom/h2
                            (dom/span "Results")
                            (if only-show-available
                              (dom/span (str " (" (count filtered-results) "/" (count results) ")"))
                              (dom/span (str " (" (count results) ")"))))
                           (if has-results
                             (->switch-input {:filters filters
                                              :key     :available
                                              :label   "Show only available"}))
                           (if fetching
                             (->fetching-indicator owner)
                             (for [movie results-to-show]
                               (dom/div ;; {:on-click #(put! feature @movie)}
                                        (movies/->movie movie))))))))
