(ns pack-builder.subs
    (:require [re-frame.core :as re-frame]))

(defn packs[db]
  (:packs db))

(defn enough-cells? [db]
  (>=
    (count (:available-cells db)) 
    (* 
      (:number-of-parallel-cells db) 
      (:number-of-series-cells db))))

(defn required-cells-message [db messages]
  (if (enough-cells? db) 
    messages 
    (conj messages {:type :error 
                    :content (str 
                               "You only have "
                               (count (:available-cells db))
                               " cells, you need at least "
                               (* (:number-of-series-cells db) (:number-of-parallel-cells db))
                               " cells to generate a "
                               (:number-of-series-cells db)
                               "S"   
                               (:number-of-parallel-cells db)
                               "P pack")})))

(defn series-cells-message [db messages]
  (if (and (> (count (:available-cells db)) 0)  (= (:number-of-series-cells db) 0))
    (conj messages {:type :warning
                    :content "You'll need to specify how many cells in series before you can generate a pack"})
    messages))

(defn parallel-cells-message [db messages]
  (if (= :fixed-cells (:pack-type db))

    (if (and (> (count (:available-cells db)) 0)  
           (= (:number-of-parallel-cells db) 0))
      (conj messages {:type :warning 
                      :content "You'll need to specify how many cells in parallel before you can generate a pack"})
      messages)
    messages))

(defn building-packs-message [db messages]
  (if (:building-packs db)
    (conj messages {:type :info 
                    :content "Building your packs..."})
    messages))

(defn messages [db]
  (->> []
      (required-cells-message db)
      (building-packs-message db)
      (series-cells-message db)
      (parallel-cells-message db)))

(defn can-generate-packs [db]
  (and 
    (> (:number-of-series-cells db) 0)
    (or (> (:number-of-parallel-cells db) 0) (= :variable-cells (:pack-type db)))
    (enough-cells? db)))

(defn packs-tsv [db]
  (let [packs (:packs db)
        max-cells (apply max (map #(count (:cells %1)) packs))]
    (clojure.string/join "\r\n"
      (map 
        (partial clojure.string/join "\t")
        (partition 
          (count packs) 
          (for [c (range max-cells) p (range (count packs))]
            (:capacity (nth (:cells (nth packs p)) c {:capacity 0}))))))))

