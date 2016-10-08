(ns pack-builder.dbscan
  (:require [clojure.set :as cset]))


(defn region-query [distance-fn data eps points p]
  (into #{}
    (filter #(< (distance-fn 
                  (get data %1) 
                  (get data p)) 
                eps) 
            points)))

(defn large-enough? [s minpts]
  (>= (count s) minpts))

(defn expand-neighbours [query-fn minpts points neighbours p]
  (let [new-neighbours (query-fn points p)]
    (if (large-enough? new-neighbours minpts)
      new-neighbours
      #{})))

(defn expand-cluster [query-fn minpts points p neighbours]
  (loop [neighbours neighbours
         points (cset/difference points neighbours)]
    (let [new-neighbours (map (partial expand-neighbours query-fn minpts points) neighbours)
          expanded-neighbours (reduce #(cset/union %1 %2) neighbours new-neighbours)
          unused-points (cset/difference points expanded-neighbours)] 
      (if (= (count expanded-neighbours) (count neighbours))
        expanded-neighbours
        (recur neighbours unused-points)))))

(defn expand-clusters [query-fn minpts clusters points p neighbours]
  (if (large-enough? neighbours minpts)
    (conj clusters (expand-cluster query-fn minpts points p neighbours))
    clusters))

(defn expand-noise [noise neighbours minpts]
  (if (large-enough? neighbours minpts)
    noise
    (cset/union noise neighbours)))

(defn filter-used-points [clusters noise points]
  (loop [c (conj clusters noise)
         p points]
    (if (empty? c)
      p
      (recur (rest c) (cset/difference p (first c))))))

(defn DBSCAN [data eps minpts distance-fn]
  (let [query-fn (partial region-query distance-fn data eps)]
    (loop [clusters []
           noise []
           points (into #{} (range (count data)))]
      (if (empty? points)
        [clusters noise]
        (let [p (first points)
              neighbours (query-fn points p)
              expanded-noise (expand-noise noise neighbours minpts)
              expanded-clusters (expand-clusters query-fn minpts clusters points p neighbours)
              points-remaining (filter-used-points expanded-clusters expanded-noise points)]
          (recur expanded-clusters expanded-noise points-remaining))))))
