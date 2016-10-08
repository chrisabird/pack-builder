(ns ^:fighweel-load pack-builder.test.subs
    (:require [cljs.test :refer-macros [deftest is testing run-tests]]
              [pack-builder.subs :as sub]))

;; can generate packs
(deftest should-be-allowed-to-generate-packs-if-enough-cells-and-pack-options-greater-than-zero
  (let [db {:available-cells [{:capacity 1000}] :number-of-series-cells 1 :number-of-parallel-cells 1}]
    (is (= true (sub/can-generate-packs db)))))

(deftest should-not-be-allowed-to-generate-packs-if-not-enough-available
  (let [db {:available-cells [{:capacity 1000} {:capacity 2000}] :number-of-series-cells 3 :number-of-parallel-cells 1}]
    (is (= false (sub/can-generate-packs db)))))

(deftest should-not-be-allowed-to-generate-packs-series-cells-is-zero
  (let [db {:available-cells [{:capacity 1000} {:capacity 2000}] :number-of-series-cells 0 :number-of-parallel-cells 1}]
    (is (= false (sub/can-generate-packs db)))))

(deftest should-not-be-allowed-to-generate-packs-parallel-cells-is-zero
  (let [db {:available-cells [{:capacity 1000} {:capacity 2000}] :number-of-series-cells 1 :number-of-parallel-cells 0}]
    (is (= false (sub/can-generate-packs db)))))

;; required cell message 

(deftest should-show-message-if-not-enough-cells
  (let [db {:available-cells [{:capacity 1000}] :number-of-series-cells 2 :number-of-parallel-cells 2}
        message (first (sub/messages db))]
    (is (= "You only have 1 cells, you need at least 4 cells to generate a 2S2P pack" (:content message)))
    (is (= :error (:type message)))))

(deftest should-not-show-message-if-enough-cells
  (let [db {:available-cells [{:capacity 1000} {:capacity 2000}] :number-of-series-cells 1 :number-of-parallel-cells 2}]
    (is (= 0 (count (sub/messages db))))))


;; parallel cell message
(deftest should-show-message-if-there-are-available-cells-but-parallel-is-zero
  (let [db {:pack-type :fixed-cells :available-cells [{:capacity 1000}] :number-of-parallel-cells 0 :number-of-series-cells 1}
        message (first (sub/messages db))]
    (is (= "You'll need to specify how many cells in parallel before you can generate a pack" (:content message)))
    (is (= :warning (:type message)))))

(deftest should-not-show-message-if-there-are-available-cells-and-parallel-is-greater-than-zero
  (let [db {:pack-type :fixed-cells :available-cells [{:capacity 1000} {:capacity 2000}] :number-of-series-cells 1 :number-of-parallel-cells 2}]
    (is (= 0 (count (sub/messages db))))))

(deftest should-not-show-message-if-there-are-no-available-cells-and-parallel-is-greater-than-zero
  (let [db {:pack-type :fixed-cells :available-cells [] :number-of-series-cells 0 :number-of-parallel-cells 1}]
    (is (= 0 (count (sub/messages db))))))

(deftest should-not-show-message-for-parallel-cells-if-pack-type-variable
  (let [db {:pack-type :variable-cells :available-cells [{:capacity 1000}] :number-of-series-cells 1 :number-of-parallel-cells 0}]
    (is (= 0 (count (sub/messages db))))))

;; parallel cell message
(deftest should-show-message-if-there-are-available-cells-but-series-is-zero
  (let [db {:available-cells [{:capacity 1000}] :number-of-parallel-cells 1 :number-of-series-cells 0}
        message (first (sub/messages db))]
    (is (= "You'll need to specify how many cells in series before you can generate a pack" (:content message)))
    (is (= :warning (:type message)))))

(deftest should-not-show-message-if-there-are-available-cells-and-series-is-greater-than-zero
  (let [db {:available-cells [{:capacity 1000} {:capacity 2000}] :number-of-series-cells 1 :number-of-parallel-cells 2}]
    (is (= 0 (count (sub/messages db))))))

(deftest should-not-show-message-if-there-are-no-available-cells-and-series-is-greater-than-zero
  (let [db {:available-cells [] :number-of-series-cells 1 :number-of-parallel-cells 0}]
    (is (= 0 (count (sub/messages db))))))


;; Building message
(deftest should-show-building-message-if-building
  (let [db {:building-packs true :available-cells [{:capacity 1000} {:capacity 1000}] :number-of-series-cells 1 :number-of-parallel-cells 2}
        message (first (sub/messages db))]
    (is (= "Building your packs..." (:content message)))
    (is (= :info (:type message)))))

(deftest should-not-show-building-message-if-not-building
  (let [db {:building-packs false :available-cells [{:capacity 1000} {:capacity 2000}] :number-of-series-cells 1 :number-of-parallel-cells 2}]
    (is (= 0 (count (sub/messages db))))))

