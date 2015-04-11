(ns filmster.components.movies
  (:require [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [filmster.utils :as utils])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defcomponent movie [{:keys [originalTitle
                             englishTitle
                             event
                             year
                             director
                             country
                             itunes-url
                             image
                             award]} owner]
  (render [_]
          (dom/div {:class "movie-result-card card"}
                   (dom/div {:class "card-content row"}
                            (dom/div {:class "col s8"}
                                     (dom/div {:class "card-title"}
                                              originalTitle)
                                     (if (utils/not-nil? englishTitle)
                                       (dom/span (str " (" englishTitle ") ")))
                                     (if (utils/not-nil? director)
                                       (dom/div  director))
                                     (dom/span year)
                                     (dom/div event)
                                     (dom/div award)
                                     (if (utils/not-nil? itunes-url)
                                       (dom/a {:href itunes-url :target "_"}
                                              "view in itunes")))
                            (dom/div {:class "col s4"}
                                     (if (utils/not-nil? image)
                                       (dom/div {:class "card-image"}
                                                (dom/img {:src image})))))
                   )))

