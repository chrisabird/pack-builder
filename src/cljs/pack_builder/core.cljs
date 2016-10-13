(ns pack-builder.core
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require [reagent.core :as reagent]
              [re-frame.core :as re-frame]
              [devtools.core :as devtools]
              [pack-builder.handlers :as handlers]
              [pack-builder.subs :as sub]
              [pack-builder.views :as views]
              [pack-builder.config :as config]))


(defn register-subs []
  (re-frame/register-sub :packs (fn [db] (reaction (sub/packs @db))))
  (re-frame/register-sub :packs-tsv (fn [db] (reaction (sub/packs-tsv @db))))
  (re-frame/register-sub :messages (fn [db] (reaction (sub/messages @db))))
  (re-frame/register-sub :can-generate-packs (fn [db] (reaction (sub/can-generate-packs @db)))))

(defn register-handlers []
  (re-frame/register-handler :initialize-db handlers/initialize-db)
  (re-frame/register-handler :type-of-pack-changed handlers/type-of-pack-changed)
  (re-frame/register-handler :available-cells-changed handlers/available-cells-changed)
  (re-frame/register-handler :number-of-series-cells-changed handlers/number-of-series-cells-changed)
  (re-frame/register-handler :number-of-parallel-cells-changed handlers/number-of-parallel-cells-changed)
  (re-frame/register-handler :cells-need-allocating handlers/cells-need-allocating)
  (re-frame/register-handler :packs-need-rebuilding handlers/packs-need-rebuilding)
  (re-frame/register-handler :allocated-cell-used handlers/allocated-cell-used))

(defn dev-setup []
  (when config/debug?
    (println "dev mode")
    (devtools/install!)))

(defn mount-root []
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (register-handlers)
  (register-subs)
  (re-frame/dispatch-sync [:initialize-db])
  (dev-setup)
  (mount-root))
