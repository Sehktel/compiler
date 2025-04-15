(defproject compiler "0.1.0-SNAPSHOT"
  :description "C-like language compiler for educational purposes"
  :url "https://github.com/yourusername/compiler"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.11.1"]]
  :main ^:skip-aot compiler.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}
             :dev {:dependencies [[org.clojure/test.check "1.1.1"]]}}
  :aliases {"test-all" ["test" "compiler.test-lexer"]}
  :test-paths ["test"]
  :source-paths ["src"]
  :resource-paths ["resources"])
