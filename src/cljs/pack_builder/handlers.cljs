(ns pack-builder.handlers
    (:require [re-frame.core :as re-frame]))

(defn mean [coll]
  (let [sum (reduce #(+ %1 (:capacity %2)) 0 coll)
        count (count coll)]
    (if (pos? count)
      (/ sum count)
      0)))

(defn standard-deviation [coll]
  (let [avg (mean coll)
        squares (for [x coll]
                  (let [x-avg (- (:capacity x) avg)]
                    (* x-avg x-avg)))
        total (count coll)]
    (-> (/ (apply + squares)
           (- total 1))
        (Math/sqrt))))

(defn total-capacity [cells]
   (reduce #(+ %1 (:capacity %2)) 0 cells))

(defn convert-cell-group-to-pack [cells average-capacity]
  (let [capacity (total-capacity cells)]
    {:id (random-uuid)
     :total-capacity capacity
     :divergence (- capacity average-capacity)
     :deviation (standard-deviation cells)
     :cells cells}))

(defn guess-next [solution]
  (let [l (- (count solution) 1)
        a (rand-int l)
        b (rand-int l)]
    (assoc (assoc (vec solution) b (nth solution a)) a (nth solution b))))

(defn cost [cells average-required p]
  (let [packs (partition p cells)]
    (reduce + (map #(+ (standard-deviation %1) (.abs js/Math (- (total-capacity %1) average-required))) packs))))

(defn acceptance-probability [old-cost new-cost average-required p t]
  (.pow js/Math (aget js/Math "E") (/ (- old-cost new-cost) t)))

(defn organise-cells [cells s p]
  (let [total-cells (* s p)
        old-solution (atom (take total-cells (sort-by :capacity > cells)))
        old-cost (atom (aget js/Number "MAX_VALUE"))
        average-required (/ (total-capacity @old-solution) s)]
    (doall 
      (for [t (take-while #(> %1 0.00001) (iterate (partial * 0.93) 1.0))
            i (range 100)
            :let [new-solution (guess-next @old-solution)
                  new-cost (cost new-solution average-required p)]
            :when (> (acceptance-probability @old-cost new-cost average-required p t) (rand))]
          (do
            (reset! old-solution new-solution)
            (reset! old-cost new-cost))))
    (map #(convert-cell-group-to-pack %1 average-required) (partition p @old-solution))))

(defn generate-packs [unused-cells number-of-packs number-of-cells-per-pack]
  (if 
    (and 
      (> number-of-packs 0)
      (> number-of-cells-per-pack 0)
      (>= (count unused-cells) (* number-of-packs number-of-cells-per-pack))) 
    (organise-cells unused-cells number-of-packs number-of-cells-per-pack)
    []))

(defn parse-capacities [capacities]
  (map js/parseInt (map clojure.string/trim (clojure.string/split capacities #","))))

(defn create-cells-from-capacities [capacities]
  (map (fn [c] {:id (random-uuid) :capacity c}) capacities))

(re-frame/register-handler
  :initialize-db
  (fn  [_ _]
    {:capacities []
     :number-of-series-cells 0  
     :number-of-parallel-cells 0
     :packs []
     :loading false}))

(re-frame/register-handler
  :update-capacities
  (fn [db [_ capacities]]
    (let [new-cells (parse-capacities capacities)]
      (conj db [:capacities new-cells]))))

(re-frame/register-handler
  :update-number-of-series-cells
  (fn [db [_ number-of-cells]]
    (let [cells (js/parseInt (clojure.string/trim number-of-cells))]
      (conj db [:number-of-series-cells cells]))))

(re-frame/register-handler
  :update-number-of-parallel-cells
  (fn [db [_ number-of-cells]]
    (let [cells (js/parseInt (clojure.string/trim number-of-cells))]
      (conj db [:number-of-parallel-cells cells]))))


(re-frame/register-handler
  :generate-packs
  (fn [db _]
    (let [unused-cells (create-cells-from-capacities (:capacities db))
          new-packs (generate-packs unused-cells (:number-of-series-cells db) (:number-of-parallel-cells db))]
      (conj db {:packs new-packs :loading false}))))


(re-frame/register-handler
  :clear-packs
  (fn [db _]
    (conj db {:packs [] :loading true})))
