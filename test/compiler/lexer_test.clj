(ns compiler.lexer-test
  (:require [clojure.test :refer :all]
            [compiler.lexer :refer [lex]]))

;; Тесты для ключевых слов
(deftest keyword-tests
  (testing "Распознавание ключевых слов"
    (let [tokens (lex "void interrupt int")]
      (is (= (map :type tokens) [:keyword :keyword :keyword]))
      (is (= (map :value tokens) ["void" "interrupt" "int"])))))

;; Тесты для идентификаторов
(deftest identifier-tests
  (testing "Распознавание идентификаторов"
    (let [tokens (lex "myVar _var1 var2")]
      (is (= (map :type tokens) [:identifier :identifier :identifier]))
      (is (= (map :value tokens) ["myVar" "_var1" "var2"])))))

;; Тесты для чисел
(deftest number-tests
  (testing "Распознавание чисел"
    (let [tokens (lex "123 0xFF 0x1A")]
      (is (= (map :type tokens) [:number :number :number]))
      (is (= (map :value tokens) ["123" "0xFF" "0x1A"])))))

;; Тесты для операторов
(deftest operator-tests
  (testing "Распознавание операторов"
    (let [tokens (lex "+ - * / % = == != ^= < > <= >= ++ -- && || ! & | ^ ~ << >>")]
      (is (= (map :type tokens) [:operator :operator :operator :operator :operator :operator 
      				 :operator :operator :operator :operator :operator :operator :operator 
      				 :operator :operator :operator :operator :operator :operator 
      				 :operator :operator :operator :operator :operator ]))
      (is (= (map :value tokens) ["+" "-" "*" "/" "%" "=" "==" "!=" "^=" "<" ">" "<=" ">=" "++" "--" "&&" "||" "!" "&" "|" "^" "~" "<<" ">>"])))))


;; Тесты для разделителей
(deftest separator-tests
  (testing "Распознавание разделителей"
    (let [tokens (lex "(){}[],;:")]
      (is (= (map :type tokens) (repeat 9 :separator)))
      (is (= (map :value tokens) ["(" ")" "{" "}" "[" "]" "," ";" ":"])))))

;; Тесты для комментариев
(deftest comment-tests
  (testing "Распознавание комментариев"
    (let [tokens (lex "// комментарий\n/* многострочный\nкомментарий */")]
      (is (= (map :type tokens) [:comment :comment])))))

;; Тест для обработчика прерывания
(deftest interrupt-handler-test
  (testing "Распознавание обработчика прерывания"
    (let [tokens (lex "void timer0_isr() interrupt 2 {
    P1 ^= 0x01;  // Инвертируем бит
}")]
      (is (= (map :type tokens) [:keyword :identifier :separator :separator :keyword :number :separator
                                :identifier :operator :number :separator :comment :separator]))
      (is (= (map :value tokens) ["void" "timer0_isr" "(" ")" "interrupt" "2" "{"
                                 "P1" "^=" "0x01" ";" "// Инвертируем бит" "}"])))))

;; Тест для сложного выражения
(deftest complex-expression-test
  (testing "Распознавание сложного выражения"
    (let [tokens (lex "if (P1 & 0x01) { PORT = 0xFF; }")]
      (is (= (map :type tokens) [:keyword :separator :identifier :operator :number :separator
                                :separator :identifier :operator :number :separator :separator]))
      (is (= (map :value tokens) ["if" "(" "P1" "&" "0x01" ")" "{"
                                 "PORT" "=" "0xFF" ";" "}"]))))) 