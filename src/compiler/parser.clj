(ns compiler.parser
  (:require [clojure.string :as str]
            [compiler.ast :refer [->Num ->BinaryOp ->Parens ->Variable ->FunctionCall 
                                 ->If ->While ->For ->Return ->UnaryOp]]
            [compiler.lexer :refer [tokenize]]))

;; Объявление функций для разрешения циклических зависимостей
(declare parse-parens parse-binary-op parse-expr parse-statement parse-block)

;; Функция для парсинга числовых литералов
(defn parse-number [tokens]
  (when-let [[token-type token-value] (first tokens)]
    (when (= token-type :number)
      [(->Num (read-string token-value)) (rest tokens)])))

;; Функция для парсинга переменных
(defn parse-variable [tokens]
  (when-let [[token-type token-value] (first tokens)]
    (when (= token-type :identifier)
      [(->Variable token-value) (rest tokens)])))

;; Функция для парсинга выражений в скобках
(defn parse-parens [tokens]
  (when-let [[token-type token-value] (first tokens)]
    (when (and (= token-type :operator) (= token-value "("))
      (when-let [[expr remaining] (parse-expr (rest tokens))]
        (when-let [[close-type close-value] (first remaining)]
          (when (and (= close-type :operator) (= close-value ")"))
            [(->Parens expr) (rest remaining)]))))))

;; Функция для парсинга бинарных операций
(defn parse-binary-op [tokens]
  (when-let [[left remaining] (parse-term tokens)]
    (when-let [[op-type op-value] (first remaining)]
      (when (contains? #{:plus :minus :asterisk :slash 
                        :equal_equal :not_equal
                        :less :less_equal :greater :greater_equal
                        :logical_and :logical_or
                        :bit_and :bit_or :bit_xor
                        :shift_left :shift_right} op-type)
        (when-let [[right remaining] (parse-term (rest remaining))]
          [(->BinaryOp op-value left right) remaining])))))

;; Функция для парсинга унарных операций
(defn parse-unary-op [tokens]
  (when-let [[op-type op-value] (first tokens)]
    (when (contains? #{:logical_not :bit_not :minus} op-type)
      (when-let [[expr remaining] (parse-term (rest tokens))]
        [(->UnaryOp op-value expr) remaining]))))

;; Функция для парсинга термов (числа, переменные, унарные операции или выражения в скобках)
(defn parse-term [tokens]
  (or (parse-unary-op tokens)
      (parse-number tokens)
      (parse-variable tokens)
      (parse-parens tokens)))

;; Функция для парсинга выражений (бинарные операции или термы)
(defn parse-expr [tokens]
  (or (parse-binary-op tokens)  
      (parse-term tokens)))

;; Функция для парсинга условного оператора if
(defn parse-if [tokens]
  (when-let [[token-type token-value] (first tokens)]
    (when (and (= token-type :if_keyword) (= token-value "if"))
      (when-let [[condition remaining] (parse-expr (rest tokens))]
        (when-let [[then remaining] (parse-statement remaining)]
          (let [[else remaining] 
                (when-let [[else-type else-value] (first remaining)]
                  (when (and (= else-type :else_keyword) (= else-value "else"))
                    (parse-statement (rest remaining))))]
            [(->If condition then else) remaining]))))))

;; Функция для парсинга цикла while
(defn parse-while [tokens]
  (when-let [[token-type token-value] (first tokens)]
    (when (and (= token-type :while_keyword) (= token-value "while"))
      (when-let [[condition remaining] (parse-expr (rest tokens))]
        (when-let [[body remaining] (parse-statement remaining)]
          [(->While condition body) remaining])))))

;; Функция для парсинга цикла for
(defn parse-for [tokens]
  (when-let [[token-type token-value] (first tokens)]
    (when (and (= token-type :for_keyword) (= token-value "for"))
      (when-let [[init remaining] (parse-expr (rest tokens))]
        (when-let [[condition remaining] (parse-expr remaining)]
          (when-let [[update remaining] (parse-expr remaining)]
            (when-let [[body remaining] (parse-statement remaining)]
              [(->For init condition update body) remaining])))))))

;; Функция для парсинга оператора return
(defn parse-return [tokens]
  (when-let [[token-type token-value] (first tokens)]
    (when (and (= token-type :return_keyword) (= token-value "return"))
      (if-let [[expr remaining] (parse-expr (rest tokens))]
        [(->Return expr) remaining]
        [(->Return nil) (rest tokens)]))))

;; Функция для парсинга блока кода
(defn parse-block [tokens]
  (when-let [[token-type token-value] (first tokens)]
    (when (and (= token-type :open_curly_bracket) (= token-value "{"))
      (loop [remaining (rest tokens)
             statements []]
        (if-let [[token-type token-value] (first remaining)]
          (if (and (= token-type :close_curly_bracket) (= token-value "}"))
            [(->Block statements) (rest remaining)]
            (if-let [[stmt remaining] (parse-statement remaining)]
              (recur remaining (conj statements stmt))
              nil))
          nil)))))

;; Функция для парсинга операторов
(defn parse-statement [tokens]
  (or (parse-if tokens)
      (parse-while tokens)
      (parse-for tokens)
      (parse-return tokens)
      (parse-block tokens)
      (parse-expr tokens)))

;; Основная функция парсинга
(defn parse [input]
  (let [tokens (tokenize input)
        [expr remaining] (parse-statement tokens)]
    (when (empty? remaining)
      expr)))
    