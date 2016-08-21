(ns pack-builder.handlers
    (:require [re-frame.core :as re-frame]
              [pack-builder.db :as db]))

(defn mean [coll]
    (let [sum (reduce #(+ %1 (:capacity %2)) 0 coll)
                  count (count coll)]
          (if (pos? count)
                  (/ sum count)
                  0)))
(defn median [& coll]
  (let [sorted (sort-by :capacity coll)
        cnt (count sorted)
        halfway (quot cnt 2)]
    (if (odd? cnt)
      (:capacity (nth sorted halfway)) ; (1)
      (let [bottom (dec halfway)
            bottom-val (nth sorted bottom)
            top-val (nth sorted halfway)]
        (mean [bottom-val top-val]))))) 

(defn standard-deviation [coll]
  (let [avg (mean coll)
        squares (for [x coll]
                  (let [x-avg (- (:capacity x) avg)]
                    (* x-avg x-avg)))
        total (count coll)]
    (-> (/ (apply + squares)
           (- total 1))
        (Math/sqrt))))

(defn distance[ca cb] 
  (let [a (:capacity ca)
        b (:capacity cb)]
  (if (< a b) (- b a) (- a b))))

(defn closest [point means distance]
    (first (sort-by #(distance % point) means)))

(defn point-groups [means data distance]
    (group-by #(closest % means distance) data))

(defn average [& list] (/ (reduce + list) (count list)))

(defn new-means [average point-groups old-means]
    (for [o old-means]
          (if (contains? point-groups o)
                  (apply average (get point-groups o)) o)))

(defn iterate-means [data distance average]
    (fn [means] (new-means average (point-groups means data distance) means)))

(defn groups [data distance means]
  (vals (point-groups means data distance)))

(defn take-while-unstable 
  ([sq] (lazy-seq (if-let [sq (seq sq)]
                    (cons (first sq) (take-while-unstable (rest sq) (first sq))))))
  ([sq last] (lazy-seq (if-let [sq (seq sq)]
                         (if (= (:capacity (first sq)) (:capacity last)) '() (take-while-unstable sq))))))

(defn k-groups [data]
  (fn [guesses]
    (take-while-unstable
      (map #(groups data distance %)
           (iterate (iterate-means data distance median) guesses)))))

(defn calculate-shortest-distances [cells centers] 
  (map (fn [cell] 
         (first 
           (min 
             (map (fn [center] 
                    (Math/pow (distance cell center) 2)) 
                  centers)))) 
       cells))

(defn new-center [distances cells]
  (let [sum (reduce + 0.0 distances)
        cums (reductions + distances)
        prob (map-indexed (fn [idx itm] [idx itm]) cums)
        r (rand)
        index (first (first (filter #(> (second %) (* r sum)) prob)))]
      (nth cells index)))

(defn init-centers [cells total]
  (let [guess-centers [(rand-nth cells)]]
    (loop [centers guess-centers]
      (if (= total (count centers))
        centers
        (let [distances (calculate-shortest-distances cells centers)
            new-centers (conj centers (new-center distances cells))]
          (recur new-centers))))))

(defn k-group [cells number-of-packs]
  (let [centers (init-centers cells number-of-packs)]
    (last ((k-groups cells) centers))))

(defn refine-to-cells-outside-two-std [unused-cells]
    (let [mean (mean unused-cells)
          std (* 2 (standard-deviation unused-cells))
          lower (- std mean)
          upper (+ std mean)]
      (filter #(or (< (:capacity %) lower) (> (:capacity %) upper)) unused-cells)))


(defn convert-cell-group-to-pack [cells]
  {:id (random-uuid)
   :total-capacity (* (count cells) (:capacity (apply min-key :capacity cells)))
   :std-deviation (standard-deviation cells)
   :median (apply median cells)
   :cells cells}) 


(defn convert-cell-groups-to-packs [cell-groups]
  (map convert-cell-group-to-pack cell-groups))

(defn generate-packs [number-of-packs unused-cells]
  (if (> (count unused-cells) 0) 
    (let [cell-groups (k-group unused-cells number-of-packs)
          packs (convert-cell-groups-to-packs cell-groups)]
      packs)
    []))


(defn parse-capacities [capacities]
  (map js/parseInt (map clojure.string/trim (clojure.string/split capacities #","))))

(defn create-cells-from-capacities [capacities]
  (map (fn [c] {:id (random-uuid) :capacity c}) capacities))

(re-frame/register-handler
  :initialize-db
  (fn  [_ _]
    {:capacities [], :unused-cells [], :packs []}))

(re-frame/register-handler
  :update-capacities
  (fn [db [_ capacities]]
    (let [new-cells (parse-capacities capacities)]
      (conj db [:capacities new-cells]))))

(re-frame/register-handler
  :add-cells
  (fn [db _]
    (let [new-cells (create-cells-from-capacities (:capacities db))]
      (conj db [:unused-cells (concat (:unused-cells db) new-cells)]))))

(re-frame/register-handler
  :generate-packs
  (fn [db _]
    (let [unused-cells (:unused-cells db)
          outliers (refine-to-cells-outside-two-std unused-cells)
          cells (remove (set outliers) unused-cells)
          old-packs (:packs db)
          generated-packs (generate-packs 2 cells)
          new-packs (concat old-packs generated-packs)]
      (conj db {:unused-cells outliers :packs new-packs}))))

(re-frame/register-handler
  :split-pack
  (fn [db [_ id]]
    (let [cells (:cells (first (filter #(= (:id %) id) (:packs db))))
          old-packs (remove #(= (:id %) id) (:packs db))
          split-packs (generate-packs 2 cells)
          new-packs (concat old-packs split-packs)]
      (conj db [:packs new-packs]))))

(re-frame/register-handler
  :unallocate-pack
  (fn [db [_ id]]
    (let [unused-cells (:unused-cells db)
          cells (:cells (first (filter #(= (:id %) id) (:packs db))))
          new-packs (remove #(= (:id %) id) (:packs db))
          new-unused-cells (concat unused-cells cells)]
      (conj db {:unused-cells new-unused-cells :packs new-packs}))))

(re-frame/register-handler
  :unallocate-cell
  (fn [db [_ ids]]
    (let [pack-id (first ids)
          cell-id (second ids)
          unused-cells (:unused-cells db)
          packs (:packs db)
          pack (first (filter #(= (:id %) pack-id) packs))
          cells (:cells pack)
          unused-cell (first (filter #(= (:id %) cell-id) cells))
          new-cells (remove #(= (:id %) cell-id) cells)
          new-pack (convert-cell-group-to-pack new-cells)
          new-packs (conj (remove #(= (:id %) pack-id) (:packs db)) new-pack)
          new-unused-cells (conj unused-cells unused-cell)]
      (println "unallocate")
      (conj db {:unused-cells new-unused-cells :packs new-packs}))))

(re-frame/register-handler
  :delete-cell
  (fn [db [_ id]]
    (let [new-cells (remove #(= (:id %) id) (:unused-cells db))]
      (conj db [:unused-cells  new-cells]))))
