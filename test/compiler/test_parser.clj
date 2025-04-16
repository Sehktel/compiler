(ns compiler.test_parser
  (:require 
   [clojure.test :refer :all]
   [compiler.parser :as parser]
   [compiler.ast :as ast]))

(deftest test-basic-parsing
  (testing "Базовый парсинг простых выражений"
    (let [inputs ["int x = 10;"
                  "return x;"
                  "int main() { return 0; }"]
          results (map parser/parse inputs)]
      (is (every? some? results) "Все входные данные должны быть успешно распарсены")
      (is (every? #(= (:type %) :Block) results) "Корневой узел должен быть блоком"))))

(deftest test-arithmetic-parsing
  (testing "Парсинг арифметических выражений"
    (let [inputs ["int z = x + y * 2;"
                  "int result = (10 + 20) * 3;"]
          results (map parser/parse inputs)]
      (is (every? some? results) "Арифметические выражения должны быть распарсены")
      (is (every? #(= (:type %) :Block) results) "Корневой узел должен быть блоком"))))

(deftest test-control-flow-parsing
  (testing "Парсинг управляющих структур"
    (let [inputs ["if (x > 0) { return 1; } else { return 0; }"
                  "while (x < 10) { x = x + 1; }"]
          results (map parser/parse inputs)]
      (is (every? some? results) "Управляющие структуры должны быть распарсены")
      (is (every? #(= (:type %) :Block) results) "Корневой узел должен быть блоком"))))

(defn run-parser-tests 
  "Запускает тесты парсера"
  []
  (run-tests 'compiler.test_parser)) 