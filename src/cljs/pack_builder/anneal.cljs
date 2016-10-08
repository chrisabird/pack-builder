(ns pack-builder.anneal)

(defn anneal [start-solution neighbour-fn cost-fn ap-fn]
  (let [solution (atom start-solution)
        cost (atom (aget js/Number "MAX_VALUE"))]
    (doall 
      (for [t (take-while #(> %1 0.000001) (iterate (partial * 0.93) 1.0))
            i (range 100)
            :let [new-solution (neighbour-fn @solution)
                  new-cost (cost-fn new-solution)]
            :when (> (ap-fn @cost new-cost t) (rand))]
          (do
            (reset! solution new-solution)
            (reset! cost new-cost))))
        @solution))
