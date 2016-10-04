(ns pack-builder.subs
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require [re-frame.core :as re-frame]))

(re-frame/register-sub
 :packs
 (fn [db]
   (reaction (:packs @db))))

(re-frame/register-sub
 :loading
 (fn [db]
   (reaction (:loading @db))))

(re-frame/register-sub
 :unused-cells
 (fn [db]
   (reaction (:unused-cells @db))))

(re-frame/register-sub
 :number-of-cells-required
 (fn [db]
   (reaction (* (:number-of-parallel-cells @db) (:number-of-series-cells @db)))))

(re-frame/register-sub
 :number-of-series-cells
 (fn [db]
   (reaction (:number-of-series-cells @db))))

(re-frame/register-sub
 :number-of-parallel-cells
 (fn [db]
   (reaction (:number-of-parallel-cells @db))))

(re-frame/register-sub
 :are-not-enough-cells
 (fn [db]
   (reaction (< 
               (count (:capacities @db)) 
               (* 
                 (:number-of-parallel-cells @db) 
                 (:number-of-series-cells @db))))))
