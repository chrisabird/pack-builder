(ns pack-builder.views
    (:require [re-frame.core :as re-frame]))


(defn mean [coll]
    (let [sum (apply + coll)
                  count (count coll)]
          (if (pos? count)
                  (/ sum count)
                  0)))

(defn standard-deviation [coll]
  (let [avg (mean coll)
        squares (for [x coll]
                  (let [x-avg (- x avg)]
                    (* x-avg x-avg)))
        total (count coll)]
    (-> (/ (apply + squares)
           (- total 1))
        (Math/sqrt))))

(defn main-panel []
  (let [outliers (re-frame/subscribe [:outliers])
        cells (re-frame/subscribe [:cells])
        pack (re-frame/subscribe [:pack])]
    (fn []
      [:div {:class "container"} 
       [:div {:class "row"}
        [:div {:class "col-md-12 "} 

         [:div {:class "form-group"}
          [:label {:class "form-label"} "Cells"]
          [:span {:class "help-block"} "Comma seperated list of cell capacities in mAH"]
          [:textarea {:type "text"
                      :class "form-control" 
                      :on-change #(re-frame/dispatch [:cells (-> % .-target .-value)]) 
                      }]]

         [:hr]

         [:div {:class "form-group"}
          [:label {:class "form-label"} "Auto group cells"]
          [:span {:class "help-block"} "Attempt to group cells by capacity into a selected number of groups (may not always give total number of requested groups)"]
          [:select {:class "form-control" :on-change #(re-frame/dispatch [:generate-capacity (-> % .-target .-value)]) }
           [:option {:value "2"} "2"]
           [:option {:value "3"} "3"]
           [:option {:value "4"} "4"]
           [:option {:value "5"} "5"]
           [:option {:value "6"} "6"]
           [:option {:value "7"} "7"]
           ]]
         [:button {:class "btn btn-default"
                   :on-click #(re-frame/dispatch [:generate-pack nil])} "Generate"]
         [:hr]

         [:h3 "Outliers"]
          [:span {:class "help-block"} "These are the cells that we're hard to group"]
         [:ul
          (for [outlier @outliers]
            [:li outlier])]
         [:hr]
         [:h3 "Packs"]
        ]]

       [:div {:class "row"}   
        (for [s @pack]
          [:div {:class "col-md-2"}
           [:p "Capacity "(apply + s)]
           [:p "Std Deviation " (Math/round (standard-deviation s))]
           [:ul
            (for [cell s]
              [:li cell])]])]]
      )))
