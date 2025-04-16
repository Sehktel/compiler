(ns test-code-generator
  (:require [clojure.test :refer :all]
            [compiler.lexer :refer [tokenize]]
            [compiler.parser :refer [parse]]
            [compiler.code-generator :refer [compile-to-8051]]))

(deftest test-simple-arithmetic
  (testing "Генерация простых арифметических операций"
    (let [code "int x = 5 + 3;"
          tokens (tokenize code)
          ast (parse code)
          assembly (compile-to-8051 ast)]
      (is (string? assembly))
      (is (not-empty assembly))
      (println "Сгенерированный ассемблер:")
      (println assembly))))

(deftest test-binary-operations
  (testing "Генерация бинарных операций"
    (let [code "int x = 10 * 2; int y = x - 5;"
          tokens (tokenize code)
          ast (parse code)
          assembly (compile-to-8051 ast)]
      (is (string? assembly))
      (is (not-empty assembly))
      (println "Сгенерированный ассемблер:")
      (println assembly))))

(deftest test-sfr-declaration
  (testing "Генерация кода с объявлением SFR"
    (let [code "sfr SP = 0x81;"
          tokens (tokenize code)
          ast (parse code)
          assembly (compile-to-8051 ast)]
      (is (string? assembly))
      (is (not-empty assembly))
      (println "Сгенерированный ассемблер:")
      (println assembly))))

(defn run-tests []
  (run-tests 'test-code-generator))

(defn -main []
  (run-tests)) 