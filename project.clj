(defproject pack-builder "0.1.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.51"]
                 [reagent "0.5.1"]
                 [binaryage/devtools "0.6.1"]
                 [funcool/promesa "1.5.0"]
                 [re-frame "0.7.0"]]

  :plugins [[lein-cljsbuild "1.1.3"]]

  :min-lein-version "2.5.3"

  :source-paths ["src/clj"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :figwheel {:css-dirs ["resources/public/css"]}

  :profiles
  {:dev
   {:dependencies []

    :plugins      [[lein-figwheel "0.5.4-3"]]
    }}

  :cljsbuild
  {:builds
   [
    {:id           "dev"
     :source-paths ["src/cljs"]
     :figwheel     {:on-jsload "pack-builder.core/mount-root"}
     :compiler     {:main                 pack-builder.core
                    :output-to            "resources/public/js/compiled/app.js"
                    :output-dir           "resources/public/js/compiled/out"
                    :asset-path           "js/compiled/out"
                    :source-map-timestamp true}}
    
    {:id           "test"
     :source-paths ["src/cljs", "test/cljs"]
     :figwheel     {:on-jsload "pack-builder.test.core/run"}
     :compiler     {:main                 pack-builder.test.core
                    :output-to            "resources/public/js/compiled/app.js"
                    :output-dir           "resources/public/js/compiled/out-test"
                    :asset-path           "js/compiled/out-test"
                    :source-map-timestamp true}}

    {:id           "min"
     :source-paths ["src/cljs"]
     :compiler     {:main            pack-builder.core
                    :output-to       "dist/web/js/compiled/app.js"
                    :optimizations   :advanced
                    :closure-defines {goog.DEBUG false}
                    :pretty-print    false}}
    
    {:id           "app"
     :source-paths ["src/cljs"]
     :compiler     {:main            pack-builder.core
                    :output-to       "app/js/compiled/app.js"
                    :optimizations   :advanced
                    :closure-defines {goog.DEBUG false}
                    :pretty-print    false}}

    ]}

  )
