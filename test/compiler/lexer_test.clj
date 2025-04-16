(ns compiler.lexer_test
  (:require [clojure.test :refer :all]
            [compiler.lexer :refer [lex]]))

;; Тесты для ключевых слов
(deftest keyword-tests
  (testing "Распознавание ключевых слов"
    (let [tokens (lex "void interrupt int char if else while for return break continue")]
      (is (= (map :type tokens) (repeat 11 :keyword)))
      (is (= (map :value tokens) ["void" "interrupt" "int" "char" "if" "else" "while" "for" "return" "break" "continue"])))))

;; Тесты для идентификаторов
(deftest identifier-tests
  (testing "Распознавание идентификаторов"
    (let [tokens (lex "myVar _var1 var2 P0 P1 P2 PORT UART")]
      (is (= (map :type tokens) (repeat 8 :identifier)))
      (is (= (map :value tokens) ["myVar" "_var1" "var2" "P0" "P1" "P2" "PORT" "UART"])))))

;; Тесты для чисел
(deftest number-tests
  (testing "Распознавание различных форматов чисел"
    (let [tokens (lex "123 0xFF 0x1A 0b1010 0 -42")]
      (is (= (map :type tokens) [:number :number :number :number :number :number]))
      (is (= (map :value tokens) ["123" "0xFF" "0x1A" "0b1010" "0" "-42"])))))

;; Тесты для операторов
(deftest operator-tests
  (testing "Распознавание операторов"
    (let [tokens (lex "+ - * / % = == != ^= < > <= >= ++ -- && || ! & | ^ ~ << >>")]
      (is (= (map :type tokens) (repeat 24 :operator)))
      (is (= (map :value tokens) ["+" "-" "*" "/" "%" "=" "==" "!=" "^=" "<" ">" "<=" ">=" "++" "--" "&&" "||" "!" "&" "|" "^" "~" "<<" ">>"]))
    )))

;; Тесты для составных операторов присваивания
(deftest compound-assignment-tests
  (testing "Распознавание составных операторов присваивания"
    (let [tokens (lex "+= -= *= /= %= &= |= ^= <<= >>=")]
      (is (= (map :type tokens) (repeat 10 :operator)))
      (is (= (map :value tokens) ["+=" "-=" "*=" "/=" "%=" "&=" "|=" "^=" "<<=" ">>="])))))

;; Тесты для разделителей
(deftest separator-tests
  (testing "Распознавание разделителей"
    (let [tokens (lex "(){}[],;:")]
      (is (= (map :type tokens) (repeat 9 :separator)))
      (is (= (map :value tokens) ["(" ")" "{" "}" "[" "]" "," ";" ":"])))))

;; Тесты для комментариев
(deftest comment-tests
  (testing "Распознавание комментариев"
    (let [tokens (lex "// однострочный комментарий\n/* многострочный\nкомментарий */")]
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

;; Тест для функции с атрибутом interrupt
(deftest attribute-interrupt-test
  (testing "Распознавание функции с атрибутом interrupt"
    (let [tokens (lex "void __attribute__((interrupt)) uart_isr(void) {
    // Обработка прерывания UART
}")]
      (is (contains? (set (map :value tokens)) "interrupt"))
      (is (contains? (set (map :value tokens)) "__attribute__")))))

;; Тест для сложного выражения if-else
(deftest complex-if-else-test
  (testing "Распознавание сложного условного выражения"
    (let [tokens (lex "if (P1 & 0x01) { 
    PORT = 0xFF; 
} else if (P1 & 0x02) {
    PORT = 0x00;
} else {
    PORT = 0xAA;
}")]
      (is (= (map :type (take 5 tokens)) [:keyword :separator :identifier :operator :number]))
      (is (= (map :value (take 5 tokens)) ["if" "(" "P1" "&" "0x01"]))
      (is (contains? (set (map :value tokens)) "else")))))

;; Тест для цикла while
(deftest while-loop-test
  (testing "Распознавание цикла while"
    (let [tokens (lex "while (count < 10) {
    buffer[count] = data;
    count++;
}")]
      (is (= (map :type (take 5 tokens)) [:keyword :separator :identifier :operator :number]))
      (is (= (map :value (take 5 tokens)) ["while" "(" "count" "<" "10"]))
      (is (contains? (set (map :value tokens)) "++")))))

;; Тест для цикла for
(deftest for-loop-test
  (testing "Распознавание цикла for"
    (let [tokens (lex "for (int i = 0; i < 100; i++) {
    sum += array[i];
}")]
      (is (= (map :type (take 8 tokens)) [:keyword :separator :keyword :identifier :operator :number :separator :identifier]))
      (is (= (map :value (take 8 tokens)) ["for" "(" "int" "i" "=" "0" ";" "i"]))
      (is (contains? (set (map :value tokens)) "++"))
      (is (contains? (set (map :value tokens)) "+=")))))

;; Тест для объявления и использования указателей
(deftest pointer-test
  (testing "Распознавание объявления и использования указателей"
    (let [tokens (lex "int *ptr = &var;
*ptr = 42;")]
      (is (= (map :type (take 6 tokens)) [:keyword :operator :identifier :operator :operator :identifier]))
      (is (= (map :value (take 6 tokens)) ["int" "*" "ptr" "=" "&" "var"]))
      (is (contains? (set (map :value tokens)) "*")))))

;; Тест для определения и вызова функций
(deftest function-test
  (testing "Распознавание определения и вызова функций"
    (let [tokens (lex "int add(int a, int b) {
    return a + b;
}

int result = add(5, 10);")]
      (is (= (map :type (take 7 tokens)) [:keyword :identifier :separator :keyword :identifier :separator :keyword]))
      (is (= (map :value (take 7 tokens)) ["int" "add" "(" "int" "a" "," "int"]))
      (is (contains? (set (map :value tokens)) "return"))
      (is (contains? (set (map :value tokens)) "result"))
      (is (contains? (set (map :value tokens)) "add")))))

;; Тест для директив препроцессора
(deftest preprocessor-test
  (testing "Распознавание директив препроцессора"
    (let [tokens (lex "#include <stdio.h>
#define MAX_SIZE 100
#ifdef DEBUG
// отладочный код
#endif")]
      (is (contains? (set (map :value tokens)) "#include"))
      (is (contains? (set (map :value tokens)) "#define"))
      (is (contains? (set (map :value tokens)) "#ifdef"))
      (is (contains? (set (map :value tokens)) "#endif")))))

;; Тест для литералов строк
(deftest string-literal-test
  (testing "Распознавание строковых литералов"
    (let [tokens (lex "char *message = \"Hello, World!\";")]
      (is (contains? (set (map :type tokens)) :string))
      (is (contains? (set (map :value tokens)) "\"Hello, World!\"")))))

;; Тест для тернарного оператора
(deftest ternary-operator-test
  (testing "Распознавание тернарного оператора"
    (let [tokens (lex "int max = (a > b) ? a : b;")]
      (is (contains? (set (map :value tokens)) "?"))
      (is (contains? (set (map :value tokens)) ":"))))) 