(ns pack-builder.views
    (:require [re-frame.core :as re-frame]
              [promesa.core :as promesa]))

(defn main-panel []
  (let [packs (re-frame/subscribe [:packs])
        messages (re-frame/subscribe [:messages])
        can-generate-packs (re-frame/subscribe [:can-generate-packs])]
    (fn []
      [:div.container-fluid 
       [:div.row
        [:div.col-md-12 

          (for [message @messages]
            (cond
              (= :error (:type message)) ^{:key (random-uuid)}[:div.alert.alert-danger (:content message)]
              (= :warning (:type message)) ^{:key (random-uuid)}[:div.alert.alert-warning (:content message)]
              :else ^{:key (random-uuid)}[:div.alert.alert-info (:content message)]))

          [:h3 "Cells"]
          [:div.form-group
            [:label.form-label "Comma seperated list of cell capacities in mAH to be added to the packs"]
            [:textarea.form-control {:on-change #(re-frame/dispatch [:available-cells-changed
                                                                     (-> % .-target .-value)]) }]]

          [:hr]
          [:h3 "Pack Options"]
            [:div.form
              [:div.form-group
                [:label {:for "numberOfSeriesCells"} "Nubmer of cell in series "]
                [:input.form-control {:type "text"  
                                      :id "numberOfSeriesCells"
                                      :on-change #(re-frame/dispatch [:number-of-series-cells-changed
                                                                      (-> % .-target .-value)])}]]
              [:div.form-group
                [:label {:for "numberOfParrallelCells"} "Number of cells in parallel "]
                [:input.form-control {:type "text"  
                                      :id "numberOfParrallelCells"
                                      :on-change #(re-frame/dispatch [:number-of-parallel-cells-changed
                                                                      (-> % .-target .-value)])}]]
              [:div.form-group
               [:div.radio
                [:label
                 [:input {:type "radio" :name "packTypeOption" :id "packTypeFixed" :defaultChecked true
                          :on-click #(re-frame/dispatch [:type-of-pack-changed "fixed-cells"])}]
                 "Arrange cells in to packs so each have similar capacity and same number of cells in parallel"]]
               [:div.radio
                [:label
                 [:input {:type "radio" :name "packTypeOption" :id "packTypeVariable"
                          :on-click #(re-frame/dispatch [:type-of-pack-changed "variable-cells"])}]
                 "Arrange cells in to packs so each have similar capacity but vary the number of cells in parallel and keep cells in a pack roughly the same capacity"]]]

              (if @can-generate-packs
                [:button {:class "btn btn-info"
                          :on-click #(do
                                      (re-frame/dispatch-sync [:packs-need-rebuilding nil])
                                      (promesa/schedule 500 (fn [] (re-frame/dispatch [:cells-need-allocating nil]))))} 
                  "Generate packs"]
              [:button {:class "btn btn-info" :disabled "disabled"} "Generate packs"])]]]

          (let [packs @packs]
            [:div.row
              (if (> (count packs) 0) [:div.col-md-12 [:h3 "Packs"] [:p "Click on cells to mark them as used as you build your pack"]] nil)
              (for [pack packs]
                ^{:key (:id pack)}
                [:div.col-md-6
                [:div {:class "well"}
                  [:p [:strong "Capacity: "] (:total-capacity pack)]
                  [:p [:strong "Divergence: "] (:divergence pack)]
                  [:p [:strong "Deviation: "] (:deviation pack)]
                    (for [cell (:cells pack)]
                       
                      (if (:used  cell)
                        ^{:key (:id cell)}[:button.btn.btn-success {:type "button" :on-click #(re-frame/dispatch [:allocated-cell-used (:id cell)])} (:capacity cell)]
                        ^{:key (:id cell)}[:button.btn.btn-default {:type "button" :on-click #(re-frame/dispatch [:allocated-cell-used (:id cell)])} (:capacity cell)]))]])])])))
