(ns pack-builder.subs
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require [re-frame.core :as re-frame]))

(re-frame/register-sub
 :packs
 (fn [db]
   (reaction (:packs @db))))

(re-frame/register-sub
 :unused-cells
 (fn [db]
   (reaction (:unused-cells @db))))
