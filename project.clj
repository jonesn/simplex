(defproject nz.co.arachnid.simplex "0.1.0-SNAPSHOT"
  :description "Simplex Example In Clojure"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure            "1.10.1"]]

  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.10"]
                                  [org.clojure/java.classpath   "0.3.0"]
                                  [midje                        "1.9.9"]]
                   :plugins      [[lein-ancient "0.6.15"]
                                  [lein-midje    "3.2.1"]]}
             :uberjar {:aot :all}}

  :repl-options {:init-ns nz.co.arachnid.simplex.core
                 :port    10000})
