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
    {:id (str (random-uuid))
     :total-capacity capacity
     :divergence (- capacity average-capacity)
     :deviation (standard-deviation cells)
     :cells (sort-by :capacity > cells)}))

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

(defn create-cells-from-capacities [capacities]
  (map (fn [c] {:id (str (random-uuid)) :capacity c}) capacities))

(defn parse-capacities [capacities]
  (create-cells-from-capacities
    (remove js/isNaN 
            (map js/parseInt 
                 (map clojure.string/trim 
                      (clojure.string/split capacities #","))))))

(defn parse-number [s]
  (let [number (js/parseInt (clojure.string/trim s))]
    (if (js/isNaN number) 0 number)))

(defn mark-as-used [id pack]
  (conj pack [:cells (map (fn [cell] 
                            (if (= (:id cell) id)
                              (conj cell [:used (not (:used cell))])
                              cell)) 
                            (:cells pack))]))

(defn initialize-db [_ _]
  {:available-cells []
   :number-of-series-cells 0  
   :number-of-parallel-cells 0
   :packs []
   :loading-packs false})

(defn available-cells-changed [db [_ capacities]]
  (conj db [:available-cells (parse-capacities capacities)]))

(defn number-of-series-cells-changed [db [_ number-of-cells]]
  (conj db [:number-of-series-cells (parse-number number-of-cells)]))

(defn number-of-parallel-cells-changed [db [_ number-of-cells]]
  (conj db [:number-of-parallel-cells (parse-number number-of-cells)]))

(defn cells-need-allocating [db _]
    (let [new-packs (generate-packs (:available-cells db) 
                                    (:number-of-series-cells db) 
                                    (:number-of-parallel-cells db))]
      (conj db {:packs new-packs :building-packs false})))

(defn allocated-cell-used [db [_ id]]
  (conj db [:packs (map #(mark-as-used id %1) (:packs db))]))

(defn packs-need-rebuilding [db _]
    (conj db {:packs [] :building-packs true}))
