(ns ^:figwheel-always pack-builder.test.core
    (:require [cljs.test :refer-macros [run-all-tests]]
              [pack-builder.test.handlers]
              [pack-builder.test.subs]))

(enable-console-print!)

(defn ^:export run
    []
    (run-all-tests #"pack-builder.test.*"))
