(ns compiler.test-lexer
  (:require [clojure.test :refer :all]
            [compiler.lexer :as lexer]))

(deftest test-lexer-basic
  "Тестирование базовой функциональности лексера"
  (let [test-code "void timer0_isr() interrupt 2 {
    // Простой обработчик прерывания
    P1 ^= 0x01;  // Инвертируем бит
}"
        tokens (lexer/lex test-code)]
    (is (= (count tokens) 10) "Правильное количество токенов")
    (is (= (:type (first tokens)) :keyword) "Первый токен - ключевое слово")
    (is (= (:value (first tokens)) "void") "Первое значение - 'void'")))

(defn print-lexer-tokens
  "Функция для печати токенов с целью визуальной отладки"
  [test-code]
  (let [tokens (lexer/lex test-code)]
    (doseq [token tokens]
      (println (format "Тип: %-10s Значение: %s" 
                       (name (:type token)) 
                       (:value token)))))) 