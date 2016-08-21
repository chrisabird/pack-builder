(ns pack-builder.views
    (:require [re-frame.core :as re-frame]))

(defn main-panel []
  (let [unused-cells (re-frame/subscribe [:unused-cells])
        packs (re-frame/subscribe [:packs])]
    (fn []
      [:div {:class "container"} 
       [:div {:class "row"}
        [:div {:class "col-md-12 "} 

          [:div.form-group
            [:label.form-label "Cell Capacities"]
            [:span.help-block "Comma seperated list of cell capacities in mAH to be added to the builder"]
            [:textarea.form-control {:on-change #(re-frame/dispatch [:update-capacities (-> % .-target .-value)]) }]]
          [:button.btn.btn-default {:on-click #(re-frame/dispatch [:add-cells nil])} "Add Cells"]

          [:hr]
          [:h3 "Unused"]
          [:span.help-block "These are the cells still available to build packs"]
          [:div.well
            (for [unused-cell @unused-cells]
              ^{:key (:id unused-cell)}
              [:div.btn-group 
                [:button.btn.btn-default.dropdown-toggle {:type "button" :data-toggle "dropdown" :aria-haspopup "true" :area-expanded "false"} (:capacity unused-cell) " " [:span.caret ""]]
                [:ul.dropdown-menu
                  [:li [:a {:on-click #(re-frame/dispatch [:delete-cell (:id unused-cell)])} "Delete"]]]])
           [:hr]
           [:button {:class "btn btn-info"
                     :on-click #(re-frame/dispatch [:generate-packs nil])} "Split cells into two packs"]]
         
          [:hr]
          [:h3 "Packs"]
          (for [pack @packs]
            ^{:key (:id pack)}
            [:div {:class "well"}
              [:p [:strong "Capacity: "] (:total-capacity pack)]
              [:p [:strong "Std Deviation "] (:std-deviation pack)]
              [:p [:strong "Median "] (:median pack)]
              (for [cell (:cells pack)]
                ^{:key (:id cell)} 
                [:div.btn-group
                  [:button.btn.btn-default.dropdown-toggle {:type "button" :data-toggle "dropdown" :aria-haspopup "true" :aria-expanded "false"} (:capacity cell) " " [:span.caret ""]]
                  [:ul.dropdown-menu
                    [:li [:a {:on-click #(re-frame/dispatch [:unallocate-cell [(:id pack) (:id cell)]])} "Unallocate"]]]])
              [:hr]
              [:button.btn.btn-info {:on-click #(re-frame/dispatch [:split-pack (:id pack)])} "Split cells into two packs" ]
              [:button.btn.btn-danger {:on-click #(re-frame/dispatch [:unallocate-pack (:id pack)])} "Unallocate All"]])]]])))
