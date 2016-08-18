(ns pack-builder.handlers
    (:require [re-frame.core :as re-frame]
              [pack-builder.db :as db]))

(defn mean [coll]
    (let [sum (apply + coll)
                  count (count coll)]
          (if (pos? count)
                  (/ sum count)
                  0)))
(defn median [& coll]
  (let [sorted (sort coll)
        cnt (count sorted)
        halfway (quot cnt 2)]
    (if (odd? cnt)
      (nth sorted halfway) ; (1)
      (let [bottom (dec halfway)
            bottom-val (nth sorted bottom)
            top-val (nth sorted halfway)]
        (mean [bottom-val top-val]))))) 

(defn standard-deviation [coll]
  (let [avg (mean coll)
        squares (for [x coll]
                  (let [x-avg (- x avg)]
                    (* x-avg x-avg)))
        total (count coll)]
    (-> (/ (apply + squares)
           (- total 1))
        (Math/sqrt))))

(defn distance[a b] = (if (< a b) (- b a) (- a b)))

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
                         (if (= (first sq) last) '() (take-while-unstable sq))))))

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

(re-frame/register-handler
 :initialize-db
 (fn  [_ _]
    (let [dbs db/default-db
         cells (:cells db/default-db)]
      (conj db/default-db [:pack []]))))

(re-frame/register-handler
  :cells
  (fn [db value]
    (let [new-cells (map js/parseInt (map clojure.string/trim (clojure.string/split (second value) #",")))]
      (conj db [:cells new-cells]))))

(re-frame/register-handler
  :generate-capacity
  (fn [db [_ value]]
    (assoc db :generate-capacity value)))

(re-frame/register-handler
  :generate-pack
  (fn [db value]
    (let [num-groups (js/parseInt (:generate-capacity db))
          mean (mean (:cells db))
          std (* 2 (standard-deviation (:cells db)))
          lower (- std mean)
          upper (+ std mean)
          outliers (filter #(or (< % lower) (> % upper)) (:cells db))
          cells (remove (set outliers) (:cells db))]
      (conj db {:outliers outliers 
                :pack (last((k-groups cells) (init-centers cells num-groups)))}))))

