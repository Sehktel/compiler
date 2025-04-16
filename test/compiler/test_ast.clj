(ns compiler.test_ast
  (:require 
   [clojure.test :refer :all]
   [compiler.parser :as parser]
   [compiler.ast :as ast]
   [clojure.java.io :as io]))

(defn read-c-file 
  "Читает содержимое C-файла для тестирования"
  [filename]
  (slurp (io/resource (str "c_sources/" filename))))

(deftest test-simple-arithmetic-ast
  (testing "Парсинг простого арифметического выражения"
    (let [c-code (read-c-file "simple_arithmetic.c")
          ast (parser/parse c-code)]
      (is (some? ast) "AST должен быть сгенерирован")
      (is (= (:type ast) :Block) "Корневой узел должен быть блоком"))))

(deftest test-control-flow-ast
  (testing "Парсинг управляющих структур"
    (let [c-code (read-c-file "control_flow.c")
          ast (parser/parse c-code)]
      (is (some? ast) "AST должен быть сгенерирован")
      (is (= (:type ast) :Block) "Корневой узел должен быть блоком")
      (is (> (count (:statements ast)) 0) "Должны быть операторы в блоке"))))

(defn run-ast-tests 
  "Запускает тесты AST"
  []
  (run-tests 'compiler.test_ast)) 