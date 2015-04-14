(ns filmster.components.movie)

(defn movie-card [{:keys [originalTitle englishTitle
                          event year director country award
                          itunes-url image]}]
  [:div.movie-result-card.card
   [:div.card-content.row
    [:div.col.s8
     [:div.card-title
      (or englishTitle originalTitle)]
     [:div director]
     [:div.yellow-text event
      [:span (str " " year)]]
     [:div.yellow-text award]
     (if itunes-url
       [:a {:href itunes-url :target "_"} [:img.itunes-link {:src "/img/itunes-logo.png"}]])
     ]
    [:div.col.s4
     (if image
       [:div.card-image [:img {:src image}]])]]])
