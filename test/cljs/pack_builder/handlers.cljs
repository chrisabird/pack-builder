(ns ^:fighweel-load pack-builder.test.handlers
    (:require [cljs.test :refer-macros [deftest is testing run-tests]]
              [pack-builder.handlers :as handlers]))


;; default states
(deftest should-have-defaults
  (let [db []
        modified-db (handlers/initialize-db db nil)]
    (is (not (:building-packs modified-db)))
    (is (= :fixed-cells (:pack-type modified-db)))
    (is (= 0 (count (:available-cells modified-db))))
    (is (= 0 (:number-of-series-cells modified-db)))
    (is (= 0 (:number-of-parallel-cells modified-db)))))


;; available cells
(deftest should-convert-a-single-capacity-into-a-single-cell
  (let [db {}
        capacities "1000"
        available-cells (:available-cells (handlers/available-cells-changed db [nil capacities]))]
    (is (= 1 (count available-cells)))
    (is (= 1000 (:capacity (first available-cells))))))

(deftest should-multiple-comman-seperated-capacities-into-cells
  (let [db {}
        capacities "1000,2000"
        available-cells (:available-cells (handlers/available-cells-changed db [nil capacities]))]
    (is (= 2 (count available-cells)))
    (is (= 1000 (:capacity (first available-cells))))
    (is (= 2000 (:capacity (last available-cells))))))

(deftest should-multiple-comman-seperated-capacities-into-cells-that-contain-whitespace
  (let [db {}
        capacities "1000    , 2000"
        available-cells (:available-cells (handlers/available-cells-changed db [nil capacities]))]
    (is (= 2 (count available-cells)))
    (is (= 1000 (:capacity (first available-cells))))
    (is (= 2000 (:capacity (last available-cells))))))

(deftest should-ignore-leading-commas-when-converting-multiple-capacities-to-cells
  (let [db {}
        capacities ",1000,2000"
        available-cells (:available-cells (handlers/available-cells-changed db [nil capacities]))]
    (is (= 2 (count available-cells)))
    (is (= 1000 (:capacity (first available-cells))))
    (is (= 2000 (:capacity (last available-cells))))))

(deftest should-ignore-trailing-commas-when-converting-multiple-capacities-to-cells
  (let [db {}
        capacities "1000,2000,"
        available-cells (:available-cells (handlers/available-cells-changed db [nil capacities]))]
    (is (= 2 (count available-cells)))
    (is (= 1000 (:capacity (first available-cells))))
    (is (= 2000 (:capacity (last available-cells))))))

(deftest should-ignore-double-commas-when-converting-multiple-capacities-to-cells
  (let [db {}
        capacities "1000,,2000"
        available-cells (:available-cells (handlers/available-cells-changed db [nil capacities]))]
    (is (= 2 (count available-cells)))
    (is (= 1000 (:capacity (first available-cells))))
    (is (= 2000 (:capacity (last available-cells))))))


;; type of pack
(deftest should-parse-type-of-pack-of-fixed-cells
  (let [db {}
        pack-type (:pack-type (handlers/type-of-pack-changed db [nil "fixed-cells"]))]
    (is (= :fixed-cells pack-type))))

(deftest should-parse-type-of-pack-of-variable-cells
  (let [db {}
        pack-type (:pack-type (handlers/type-of-pack-changed db [nil "variable-cells"]))]
    (is (= :variable-cells pack-type))))

;; series cells
(deftest should-parse-number-of-series-cells
  (let [db {}
        number (:number-of-series-cells (handlers/number-of-series-cells-changed db [nil "1"]))]
    (is (= 1 number))))

(deftest should-parse-number-of-series-cells-that-includes-white-space
  (let [db {}
        number (:number-of-series-cells (handlers/number-of-series-cells-changed db [nil "1  "]))]
    (is (= 1 number))))

(deftest should-parse-numer-of-series-cells-as-zero-if-not-a-number
  (let [db {}
        number (:number-of-series-cells (handlers/number-of-series-cells-changed db [nil "a"]))]
    (is (= 0 number))))


;; parallel cells
(deftest should-parse-number-of-parallel-cells
  (let [db {}
        number (:number-of-parallel-cells (handlers/number-of-parallel-cells-changed db [nil "1"]))]
    (is (= 1 number))))

(deftest should-parse-number-of-parallel-cells-that-includes-white-space
  (let [db {}
        number (:number-of-parallel-cells (handlers/number-of-parallel-cells-changed db [nil "1  "]))]
    (is (= 1 number))))

(deftest should-parse-numer-of-parallel-cells-as-zero-if-not-a-number
  (let [db {}
        number (:number-of-parallel-cells (handlers/number-of-parallel-cells-changed db [nil "a"]))]
    (is (= 0 number))))

;; used cell
(deftest should-mark-cell-as-used
  (let [db {:packs [{:cells [{:id "1"}]}]}
        modified-db (handlers/allocated-cell-used db [nil "1"])]
    (is (:used (first (:cells (first (:packs modified-db))))))))

(deftest should-mark-cell-as-unused-if-already-used
  (let [db {:packs [{:cells [{:id "1" :used true}]}]}
        modified-db (handlers/allocated-cell-used db [nil "1"])]
    (is (not (:used (first (:cells (first (:packs modified-db)))))))))

;; allocate cells
(deftest should-be-able-t-allocate-cells-to-packs-in-series-and-parallel-dimensions
  (let [db {:pack-type :fixed-cells :available-cells [{:capacity 1000} {:capacity 2000} {:capacity 1999} {:capacity 999}] :number-of-series-cells 2 :number-of-parallel-cells 2}
        modified-db (handlers/cells-need-allocating db [nil nil])]
    (is (= 2 (count (:packs modified-db))))
    (is (= 2 (count (:cells (first (:packs modified-db))))))
    (is (= 2 (count (:cells (first (:packs modified-db))))))))

;; allocate cells
(deftest should-be-able-t-allocate-cells-to-packs-in-series-with-varying-parallel-dimensions
  (let [db {:pack-type :variable-cells :available-cells [{:capacity 2010} {:capacity 2015} {:capacity 1900} {:capacity 1910} {:capacity 2010} {:capacity 2015} {:capacity 1900} {:capacity 1910}] :number-of-series-cells 2 :number-of-parallel-cells 2}
        modified-db (handlers/cells-need-allocating db [nil nil])]
    (is (= 2 (count (:packs modified-db))))
    (is (= 4 (count (:cells (first (:packs modified-db))))))
    (is (= 4 (count (:cells (first (:packs modified-db))))))))
