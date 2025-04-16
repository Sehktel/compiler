(defproject c-compiler "0.1.0-SNAPSHOT"
  :description "Educational C Compiler Project"
  :url "https://github.com/Sehktel/cc_c51"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"}
  
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/test.check "1.1.1"]]
  
  :source-paths ["src"]
  :test-paths ["test"]
  
  :main compiler.core
  
  :plugins [[lein-codox "0.10.8"]]
  
  :codox {:output-path "docs"
          :source-paths ["src"]
          :source-uri "https://github.com/yourusername/c-compiler/blob/master/{filepath}#L{line}"
          :metadata {:doc/format :markdown}
          :namespaces [compiler.lexer 
                       compiler.parser 
                       compiler.ast]
          :themes [:default]
          :doc-files ["README.md"]
          :description "Documentation for C-like Compiler Educational Project"
          :project {:name "C-like Compiler"
                    :version "0.1.0"
                    :description "Educational project for building a C-like compiler in Clojure"}
          :exclude-vars #"^(map|filter|reduce)$"}
  
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "1.4.4"]
                                  [midje "1.10.9"]
                                  [pjstadig/humane-test-output "0.11.0"]]
                   :plugins [[lein-cloverage "1.2.4"]
                             [lein-kibit "0.1.8"]
                             [com.jakemccrary/lein-test-refresh "0.25.0"]
                             [lein-midje "3.2.1"]]
                   :injections [(require 'pjstadig.humane-test-output)
                                (pjstadig.humane-test-output/activate!)]}
             :test {:resource-paths ["test/resources"]}
             :test-8051 {:main test-8051
                        :source-paths ["src" "test"]}
             :uberjar {:aot :all
                      :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}}
  
  :aliases {"ast-test" ["run" "-m" "user/run-ast-tests"]
            "parse-file" ["run" "-m" "user/parse-file"]
            "coverage" ["with-profile" "test" "cloverage"]
            "lint" ["with-profile" "dev" "kibit"]
            
            ;; Специализированные тесты
            "test-lexer" ["test" "compiler.test-lexer"]
            "test-parser" ["test" "compiler.test-parser"]
            "test-ast" ["test" "compiler.test-ast"]
            "test-8051" ["with-profile" "test-8051" "run"]
            
            ;; Демонстрационные команды
            "demo-lexer" ["run" "-m" "compiler.lexer/print-lexer-tokens"]
            "demo-parser" ["run" "-m" "compiler.parser/parse-demo"]
            
            ;; Документация
            "docs" ["codox"]}
  
  :repl-options {:init-ns user})
