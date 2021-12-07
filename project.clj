(defproject map-matching "0.1.0-SNAPSHOT"
  :plugins [[lein-tools-deps "0.4.5"]]
  :middleware [lein-tools-deps.plugin/resolve-dependencies-with-deps-edn]
  :lein-tools-deps/config {:config-files [:install "deps-server.edn"]
                           :clojure-executables ["/opt/homebrew/bin/clojure"]}

  :profiles {:dev {:lein-tools-deps/config {:config-files ["deps.edn"]}}}

  :repl-options {:init-ns map-matching.server.main}
  :main map-matching.server.main)
