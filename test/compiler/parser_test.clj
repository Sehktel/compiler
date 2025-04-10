(ns compiler.parser-test
  (:require [clojure.test :refer :all]
            [compiler.parser :refer [parse parse-number parse-parens parse-binary-op parse-expr]]
            [compiler.lexer :refer [tokenize]]
            [compiler.ast :refer [->Num ->BinaryOp ->Parens print-ast]]))

(deftest tokenize-test
  (testing "Токенизация базовых выражений"
    (is (= [[:number "1"] [:operator "+"] [:number "2"]] (tokenize "1+2")))
    (is (= [[:open-paren "("] [:number "1"] [:operator "+"] [:number "2"] [:close-paren ")"] [:operator "*"] [:number "3"]] (tokenize "(1+2)*3")))
    (is (= [[:number "-42"] [:operator "/"] [:number "2"]] (tokenize "-42/2")))))

(deftest parse-number-test
  (testing "Парсинг числовых литералов"
    (is (= [(->Num 42) []] (parse-number [[:number "42"]])))
    (is (= [(->Num -42) []] (parse-number [[:number "-42"]])))
    (is (nil? (parse-number [[:identifier "abc"]])))))

(deftest parse-parens-test
  (testing "Парсинг выражений в скобках"
    (is (= [(->Parens (->Num 42)) []] (parse-parens [[:open-paren "("] [:number "42"] [:close-paren ")"]])))
    (is (= [(->Parens (->BinaryOp "+" (->Num 1) (->Num 2))) []]
           (parse-parens [[:open-paren "("] [:number "1"] [:operator "+"] [:number "2"] [:close-paren ")"]])))
    (is (nil? (parse-parens [[:open-paren "("] [:number "42"]])))))

(deftest parse-binary-op-test
  (testing "Парсинг бинарных операций"
    (is (= [(->BinaryOp "+" (->Num 1) (->Num 2)) []]
           (parse-binary-op [[:number "1"] [:operator "+"] [:number "2"]])))
    (is (= [(->BinaryOp "*" (->Num 3) (->Num 4)) []]
           (parse-binary-op [[:number "3"] [:operator "*"] [:number "4"]])))))

(deftest parse-test
  (testing "Парсинг полных выражений"
    (is (= (->Num 42) (parse "42")))
    (is (= (->BinaryOp "+" (->Num 1) (->Num 2)) (parse "1+2")))
    (is (= (->BinaryOp "*" 
                      (->Parens (->BinaryOp "+" (->Num 1) (->Num 2)))
                      (->Num 3))
           (parse "(1+2)*3")))
    (is (nil? (parse "1+")))))

(deftest operator-precedence-test
  (testing "Проверка приоритета операторов"
    ;; Проверка, что выражение 1+2*3 правильно парсится с учетом приоритета операторов
    (let [ast (parse "1+2*3")]
      (println "AST для 1+2*3:" ast)
      (is (= (->BinaryOp "+" (->Num 1) (->BinaryOp "*" (->Num 2) (->Num 3))) ast)))
    
    ;; Проверка, что выражение (1+2)*3 правильно парсится с учетом скобок
    (let [ast (parse "(1+2)*3")]
      (println "AST для (1+2)*3:" ast)
      (is (instance? compiler.ast.BinaryOp ast))
      (is (= "*" (:op ast)))
      (is (instance? compiler.ast.Parens (:left ast)))
      (let [parens-expr (:expr (:left ast))]
        (is (instance? compiler.ast.BinaryOp parens-expr))
        (is (= "+" (:op parens-expr)))
        (is (= (->Num 1) (:left parens-expr)))
        (is (= (->Num 2) (:right parens-expr))))
      (is (= (->Num 3) (:right ast))))))

(deftest error-handling-test
  (testing "Обработка ошибок парсинга"
    (is (nil? (parse "1+")))
    (is (nil? (parse "(1+2")))
    (is (nil? (parse "1+2)")))
    (is (nil? (parse "abc")))))

(deftest full-pipeline-test
  (testing "Полный пайплайн: лексический анализ -> парсинг -> AST"
    ;; Тест 1: Простое числовое выражение
    (let [input "42"
          tokens (tokenize input)
          ast (parse input)]
      (println "\nТест 1: Простое числовое выражение")
      (println "Входная строка:" input)
      (println "Токены:" tokens)
      (println "AST:" ast)
      (is (= (->Num 42) ast)))

    ;; Тест 2: Бинарная операция
    (let [input "1+2"
          tokens (tokenize input)
          ast (parse input)]
      (println "\nТест 2: Бинарная операция")
      (println "Входная строка:" input)
      (println "Токены:" tokens)
      (println "AST:" ast)
      (is (= (->BinaryOp "+" (->Num 1) (->Num 2)) ast)))

    ;; Тест 3: Выражение со скобками
    (let [input "(1+2)*3"
          tokens (tokenize input)
          ast (parse input)]
      (println "\nТест 3: Выражение со скобками")
      (println "Входная строка:" input)
      (println "Токены:" tokens)
      (println "AST:" ast)
      (is (= (->BinaryOp "*" 
                        (->Parens (->BinaryOp "+" (->Num 1) (->Num 2)))
                        (->Num 3))
             ast)))

    ;; Тест 4: Комплексное выражение
    (let [input "1+2*3"
          tokens (tokenize input)
          ast (parse input)]
      (println "\nТест 4: Комплексное выражение")
      (println "Входная строка:" input)
      (println "Токены:" tokens)
      (println "AST:" ast)
      (is (= (->BinaryOp "+" 
                        (->Num 1)
                        (->BinaryOp "*" (->Num 2) (->Num 3)))
             ast)))))

;; Функция для ручного тестирования
(defn test-pipeline [input]
  (println "\nТестирование пайплайна для:" input)
  (let [tokens (tokenize input)
        ast (parse input)]
    (println "Токены:" tokens)
    (println "\nAST структура:")
    (print-ast ast)
    ast))
