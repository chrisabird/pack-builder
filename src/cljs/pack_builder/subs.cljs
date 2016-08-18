(ns pack-builder.subs
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require [re-frame.core :as re-frame]))

(re-frame/register-sub
 :cells
 (fn [db]
   (reaction (:cells @db))))

(re-frame/register-sub
 :pack
 (fn [db]
   (reaction (:pack @db))))

(re-frame/register-sub
 :outliers
 (fn [db]
   (reaction (:outliers @db))))
