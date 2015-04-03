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
                             award]} owner]
  (render [_]
          (dom/div {:class "movie-result-card card"}
                   (dom/div {:class "card-content"}
                            (dom/div {:class "card-title"}
                                     originalTitle)
                            (if (utils/not-nil? englishTitle)
                              (dom/span (str " (" englishTitle ") ")))
                            (if (utils/not-nil? director)
                              (dom/div  director))
                            (dom/span year)
                            (dom/div event)
                            (dom/div award))
                   )))

