(defproject compiler "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://github.com/Sehktel/c2"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/tools.reader "1.3.0"]]
  :plugins [[lein-codox "0.10.8"]]
  :main ^:skip-aot compiler.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
