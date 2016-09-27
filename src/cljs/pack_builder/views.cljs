(ns pack-builder.views
    (:require [re-frame.core :as re-frame]
              [promesa.core :as promesa]))

(defn main-panel []
  (let [unused-cells (re-frame/subscribe [:unused-cells])
        packs (re-frame/subscribe [:packs])
        loading (re-frame/subscribe [:loading])]
    (fn []
      [:div.container-fluid 
       [:div.row
        [:div.col-md-12 

        [:h3 "Cells"]
        [:div.form-group
          [:label.form-label "Comma seperated list of cell capacities in mAH to be added to the packs"]
          [:textarea.form-control {:on-change #(re-frame/dispatch [:update-capacities (-> % .-target .-value)]) }]]

        [:hr]
        [:h3 "Packs Options"]
         [:div.form
          [:div.form-group
            [:label {:for "numberOfSeriesCells"} "Numer of cell in series "]
            [:input.form-control {:type "text"  
                                  :id "numberOfSeriesCells"
                                  :on-change #(re-frame/dispatch [:update-number-of-series-cells (-> % .-target .-value)])}]]
          [:div.form-group
            [:label {:for "numberOfParrallelCells"} "Number of cells in parrallel "]
            [:input.form-control {:type "text"  
                                  :id "numberOfParrallelCells"
                                  :on-change #(re-frame/dispatch [:update-number-of-parrallel-cells (-> % .-target .-value)])}]]

         [:button {:class "btn btn-info"
                   :on-click #(do
                                (re-frame/dispatch-sync [:clear-packs nil])
                                (promesa/schedule 500 (fn [] (re-frame/dispatch [:generate-packs nil]))))} "Generate packs"]]
         
          [:hr]
          (let [is-loading @loading]
            (if is-loading 
            [:div
             [:h3 "Packs"]
              [:div.well
                [:p.lead.text-center "Building you're packs..."]]]
            [:h3 "Packs"]))

          (for [pack @packs]
            ^{:key (:id pack)}
            [:div {:class "well"}
              [:p [:strong "Capacity: "] (:total-capacity pack)]
              [:p [:strong "Divergence: "] (:divergence pack)]
              [:p [:strong "Deviation: "] (:deviation pack)]
              (for [cell (:cells pack)]
                ^{:key (:id cell)} 
                  [:button.btn.btn-default {:type "button"} (:capacity cell)])
              [:hr]
             ])]]])))
