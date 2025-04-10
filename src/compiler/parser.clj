(ns compiler.parser
  (:require [clojure.string :as str]
            [compiler.ast :refer [->Num ->BinaryOp ->Parens ->Variable ->FunctionCall 
                                 ->If ->While ->For ->Return ->UnaryOp ->Block]]
            [compiler.lexer :refer [tokenize]]))

;; Предварительное объявление всех функций
(declare parse-if)
(declare parse-unary-op)
(declare parse-term)
(declare parse-expr)
(declare parse-statement)
(declare parse)

;; =============================================
;; Вспомогательные функции для PEG парсера
;; =============================================

(defn match-token
  "Проверяет соответствие токена заданному предикату.
   
   ## Параметры
   - `pred` - функция-предикат, принимающая тип и значение токена
   - `tokens` - последовательность токенов
   
   ## Возвращает
   Вектор `[значение_токена оставшиеся_токены]` или `nil`, если токен не соответствует предикату.
   
   ## Пример
   ```clojure
   (match-token (fn [t v] (= t :number)) [[:number \"42\"] [:plus \"+\"]])
   ;; => [\"42\" [[:plus \"+\"]]]
   ```"
  [pred tokens]
  (when-let [[token-type token-value] (first tokens)]
    (when (pred token-type token-value)
      [token-value (rest tokens)])))

(defn match-type
  "Проверяет тип токена.
   
   ## Параметры
   - `type` - ожидаемый тип токена (например, :number, :identifier)
   - `tokens` - последовательность токенов
   
   ## Пример
   ```clojure
   (match-type :number [[:number \"42\"] [:plus \"+\"]])
   ;; => [\"42\" [[:plus \"+\"]]]
   ```"
  [type tokens]
  (match-token (fn [t _] (= t type)) tokens))

(defn match-value
  "Проверяет тип и значение токена одновременно.
   
   ## Параметры
   - `type` - ожидаемый тип токена
   - `value` - ожидаемое значение токена
   - `tokens` - последовательность токенов
   
   ## Пример
   ```clojure
   (match-value :operator \"+\" [[:operator \"+\"] [:number \"42\"]])
   ;; => [\"+\" [[:number \"42\"]]]
   ```"
  [type value tokens]
  (match-token (fn [t v] (and (= t type) (= v value))) tokens))

;; =============================================
;; Базовые правила парсера
;; =============================================

(defn parse-number
  "Разбирает числовой литерал.
   
   ## Параметры
   - `tokens` - последовательность токенов
   
   ## Возвращает
   Вектор `[(->Num значение) оставшиеся_токены]` или `nil`, если токен не является числом.
   
   ## Пример
   ```clojure
   (parse-number [[:number \"42\"] [:plus \"+\"]])
   ;; => [(->Num 42) [[:plus \"+\"]]]
   ```"
  [tokens]
  (when-let [[value remaining] (match-type :number tokens)]
    [(->Num (read-string value)) remaining]))

(defn parse-variable
  "Разбирает идентификатор переменной.
   
   ## Параметры
   - `tokens` - последовательность токенов
   
   ## Возвращает
   Вектор `[(->Variable имя) оставшиеся_токены]` или `nil`, если токен не является идентификатором.
   
   ## Пример
   ```clojure
   (parse-variable [[:identifier \"x\"] [:equal \"=\"]])
   ;; => [(->Variable \"x\") [[:equal \"=\"]]]
   ```"
  [tokens]
  (when-let [[value remaining] (match-type :identifier tokens)]
    [(->Variable value) remaining]))

(defn parse-parens
  "Разбирает выражение в круглых скобках.
   
   ## Параметры
   - `tokens` - последовательность токенов
   
   ## Возвращает
   Вектор `[(->Parens выражение) оставшиеся_токены]` или `nil`, если выражение не заключено в скобки.
   
   ## Пример
   ```clojure
   (parse-parens [[:open_round_bracket \"(\"] [:number \"42\"] [:close_round_bracket \")\"]])
   ;; => [(->Parens (->Num 42)) []]
   ```"
  [tokens]
  (when-let [[_ remaining] (match-value :open_round_bracket "(" tokens)]
    (when-let [[expr remaining] (parse-expr remaining)]
      (when-let [[_ remaining] (match-value :close_round_bracket ")" remaining)]
        [(->Parens expr) remaining]))))

;; =============================================
;; Операторы
;; =============================================

(def binary-op-types
  "Множество типов бинарных операторов.
   Используется для быстрой проверки принадлежности токена к оператору."
  #{:plus :minus :asterisk :slash 
    :equal_equal :not_equal
    :less :less_equal :greater :greater_equal
    :logical_and :logical_or
    :bit_and :bit_or :bit_xor
    :shift_left :shift_right})

(def unary-op-types
  "Множество типов унарных операторов."
  #{:logical_not :bit_not :minus})

(defn parse-binary-op
  "Разбирает бинарную операцию.
   
   ## Параметры
   - `tokens` - последовательность токенов
   
   ## Возвращает
   Вектор `[(->BinaryOp оператор левый_операнд правый_операнд) оставшиеся_токены]` или `nil`.
   
   ## Пример
   ```clojure
   (parse-binary-op [[:number \"1\"] [:plus \"+\"] [:number \"2\"]])
   ;; => [(->BinaryOp \"+\" (->Num 1) (->Num 2)) []]
   ```"
  [tokens]
  (when-let [[left remaining] (parse-term tokens)]
    (when-let [[op remaining] (match-token 
                              (fn [t _] (contains? binary-op-types t))
                              remaining)]
      (when-let [[right remaining] (parse-term remaining)]
        [(->BinaryOp op left right) remaining]))))

(defn parse-unary-op
  "Разбирает унарную операцию.
   
   ## Параметры
   - `tokens` - последовательность токенов
   
   ## Возвращает
   Вектор `[(->UnaryOp оператор операнд) оставшиеся_токены]` или `nil`.
   
   ## Пример
   ```clojure
   (parse-unary-op [[:minus \"-\"] [:number \"42\"]])
   ;; => [(->UnaryOp \"-\" (->Num 42)) []]
   ```"
  [tokens]
  (when-let [[op remaining] (match-token 
                            (fn [t _] (contains? unary-op-types t))
                            tokens)]
    (when-let [[expr remaining] (parse-term remaining)]
      [(->UnaryOp op expr) remaining])))

;; =============================================
;; Термы и выражения
;; =============================================

(def parse-if
  "Разбирает условный оператор if."
  (fn [tokens]
    (when-let [[_ remaining] (match-value :if_keyword "if" tokens)]
      (when-let [[condition remaining] (parse-expr remaining)]
        (when-let [[then remaining] (parse-statement remaining)]
          (let [[else remaining] 
                (when-let [[_ remaining] (match-value :else_keyword "else" remaining)]
                  (parse-statement remaining))]
            [(->If condition then else) remaining]))))))

(defn parse-term
  "Разбирает терм (базовый элемент выражения).
   Порядок правил определяет приоритет разбора.
   
   ## Параметры
   - `tokens` - последовательность токенов
   
   ## Возвращает
   Результат первого успешного разбора из:
   - унарная операция
   - число
   - переменная
   - выражение в скобках
   
   ## Пример
   ```clojure
   (parse-term [[:number \"42\"]])
   ;; => [(->Num 42) []]
   ```"
  [tokens]
  (or (parse-unary-op tokens)   ; Сначала пробуем унарные операции
      (parse-number tokens)     ; Затем числа
      (parse-variable tokens)   ; Затем переменные
      (parse-parens tokens)))   ; И наконец выражения в скобках

(defn parse-expr
  "Разбирает выражение.
   
   ## Параметры
   - `tokens` - последовательность токенов
   
   ## Возвращает
   Результат первого успешного разбора из:
   - бинарная операция
   - терм
   
   ## Пример
   ```clojure
   (parse-expr [[:number \"1\"] [:plus \"+\"] [:number \"2\"]])
   ;; => [(->BinaryOp \"+\" (->Num 1) (->Num 2)) []]
   ```"
  [tokens]
  (or (parse-binary-op tokens)  ; Сначала пробуем бинарные операции
      (parse-term tokens)))     ; Если не получилось - пробуем терм

;; =============================================
;; Управляющие конструкции
;; =============================================

;; (defn parse-if
;;   "Разбирает условный оператор if.
   
;;    ## Параметры
;;    - `tokens` - последовательность токенов
   
;;    ## Возвращает
;;    Вектор `[(->If условие then else) оставшиеся_токены]` или `nil`.
   
;;    ## Пример
;;    ```clojure
;;    (parse-if [[:if_keyword \"if\"] [:open_round_bracket \"(\"] 
;;               [:identifier \"x\"] [:greater \">\"] [:number \"0\"] 
;;               [:close_round_bracket \")\"] [:open_curly_bracket \"{\"] 
;;               [:return_keyword \"return\"] [:number \"1\"] [:semicolon \";\"] 
;;               [:close_curly_bracket \"}\"] [:else_keyword \"else\"] 
;;               [:open_curly_bracket \"{\"] [:return_keyword \"return\"] 
;;               [:number \"0\"] [:semicolon \";\"] [:close_curly_bracket \"}\"]])
;;    ;; => [(->If (->BinaryOp \">\" (->Variable \"x\") (->Num 0))
;;    ;;           (->Block [(->Return (->Num 1))])
;;    ;;           (->Block [(->Return (->Num 0))]))
;;    ;;      []]
;;    ```"
;;   [tokens]
;;   (when-let [[_ remaining] (match-value :if_keyword "if" tokens)]
;;     (when-let [[condition remaining] (parse-expr remaining)]
;;       (when-let [[then remaining] (parse-statement remaining)]
;;         (let [[else remaining] 
;;               (when-let [[_ remaining] (match-value :else_keyword "else" remaining)]
;;                 (parse-statement remaining))]
;;           [(->If condition then else) remaining])))))

(defn parse-while
  "Разбирает цикл while.
   
   ## Параметры
   - `tokens` - последовательность токенов
   
   ## Возвращает
   Вектор `[(->While условие тело) оставшиеся_токены]` или `nil`.
   
   ## Пример
   ```clojure
   (parse-while [[:while_keyword \"while\"] [:open_round_bracket \"(\"] 
                 [:identifier \"x\"] [:greater \">\"] [:number \"0\"] 
                 [:close_round_bracket \")\"] [:open_curly_bracket \"{\"] 
                 [:identifier \"x\"] [:equal \"=\"] [:identifier \"x\"] 
                 [:minus \"-\"] [:number \"1\"] [:semicolon \";\"] 
                 [:close_curly_bracket \"}\"]])
   ;; => [(->While (->BinaryOp \">\" (->Variable \"x\") (->Num 0))
   ;;              (->Block [(->BinaryOp \"=\" (->Variable \"x\") 
   ;;                                    (->BinaryOp \"-\" (->Variable \"x\") (->Num 1)))]))
   ;;      []]
   ```"
  [tokens]
  (when-let [[_ remaining] (match-value :while_keyword "while" tokens)]
    (when-let [[condition remaining] (parse-expr remaining)]
      (when-let [[body remaining] (parse-statement remaining)]
        [(->While condition body) remaining]))))

;; (defn parse-for
;;   "Разбирает цикл for.
   
;;    ## Параметры
;;    - `tokens` - последовательность токенов
   
;;    ## Возвращает
;;    Вектор `[(->For инициализация условие обновление тело) оставшиеся_токены]` или `nil`.
   
;;    ## Пример
;;    ```clojure
;;    (parse-for [[:for_keyword \"for\"] [:open_round_bracket \"(\"] 
;;                [:identifier \"i\"] [:equal \"=\"] [:number \"0\"] [:semicolon \";\"] 
;;                [:identifier \"i\"] [:less \"<\"] [:number \"10\"] [:semicolon \";\"] 
;;                [:identifier \"i\"] [:equal \"=\"] [:identifier \"i\"] [:plus \"+\"] 
;;                [:number \"1\"] [:close_round_bracket \")\"] [:open_curly_bracket \"{\"] 
;;                [:close_curly_bracket \"}\"]])
;;    ;; => [(->For (->BinaryOp \"=\" (->Variable \"i\") (->Num 0))
;;    ;;            (->BinaryOp \"<\" (->Variable \"i\") (->Num 10))
;;    ;;            (->BinaryOp \"=\" (->Variable \"i\") 
;;    ;;                        (->BinaryOp \"+\" (->Variable \"i\") (->Num 1)))
;;    ;;            (->Block []))
;;    ;;      []]
;;    ```"
;;   [tokens]
;;   (when-let [[_ remaining] (match-value :for_keyword "for" tokens)]
;;     (when-let [[init remaining] (parse-expr remaining)]
;;       (when-let [[condition remaining] (parse-expr remaining)]
;;         (when-let [[update remaining] (parse-expr remaining)]
;;           (when-let [[body remaining] (parse-statement remaining)]
;;             [(->For init condition update body) remaining]))))))

(defn parse-return
  "Разбирает оператор return.
   
   ## Параметры
   - `tokens` - последовательность токенов
   
   ## Возвращает
   Вектор `[(->Return выражение) оставшиеся_токены]` или `nil`.
   
   ## Пример
   ```clojure
   (parse-return [[:return_keyword \"return\"] [:number \"42\"] [:semicolon \";\"]])
   ;; => [(->Return (->Num 42)) [[:semicolon \";\"]]]
   ```"
  [tokens]
  (when-let [[_ remaining] (match-value :return_keyword "return" tokens)]
    (if-let [[expr remaining] (parse-expr remaining)]
      [(->Return expr) remaining]
      [(->Return nil) remaining])))

(defn parse-block
  "Разбирает блок кода в фигурных скобках.
   
   ## Параметры
   - `tokens` - последовательность токенов
   
   ## Возвращает
   Вектор `[(->Block [операторы]) оставшиеся_токены]` или `nil`.
   
   ## Пример
   ```clojure
   (parse-block [[:open_curly_bracket \"{\"] 
                 [:identifier \"x\"] [:equal \"=\"] [:number \"1\"] [:semicolon \";\"] 
                 [:identifier \"y\"] [:equal \"=\"] [:number \"2\"] [:semicolon \";\"] 
                 [:close_curly_bracket \"}\"]])
   ;; => [(->Block [(->BinaryOp \"=\" (->Variable \"x\") (->Num 1))
   ;;               (->BinaryOp \"=\" (->Variable \"y\") (->Num 2))])
   ;;      []]
   ```"
  [tokens]
  (when-let [[_ remaining] (match-value :open_curly_bracket "{" tokens)]
    (loop [remaining remaining
           statements []]
      (if-let [[_ remaining] (match-value :close_curly_bracket "}" remaining)]
        [(->Block statements) remaining]
        (if-let [[stmt remaining] (parse-statement remaining)]
          (recur remaining (conj statements stmt))
          nil)))))

(defn parse-for
  "Разбирает цикл for.
   
   ## Параметры
   - `tokens` - последовательность токенов
   
   ## Возвращает
   Вектор `[(->For инициализация условие обновление тело) оставшиеся_токены]` или `nil`.
   
   ## Пример
   ```clojure
   (parse-for [[:for_keyword \"for\"] [:open_round_bracket \"(\"] 
               [:identifier \"i\"] [:equal \"=\"] [:number \"0\"] [:semicolon \";\"] 
               [:identifier \"i\"] [:less \"<\"] [:number \"10\"] [:semicolon \";\"] 
               [:identifier \"i\"] [:equal \"=\"] [:identifier \"i\"] [:plus \"+\"] 
               [:number \"1\"] [:close_round_bracket \")\"] [:open_curly_bracket \"{\"] 
               [:close_curly_bracket \"}\"]])
   ;; => [(->For (->BinaryOp \"=\" (->Variable \"i\") (->Num 0))
   ;;            (->BinaryOp \"<\" (->Variable \"i\") (->Num 10))
   ;;            (->BinaryOp \"=\" (->Variable \"i\") 
   ;;                        (->BinaryOp \"+\" (->Variable \"i\") (->Num 1)))
   ;;            (->Block []))
   ;;      []]
   ```"
  [tokens]
  (when-let [[_ remaining] (match-value :for_keyword "for" tokens)]
    (when-let [[_ remaining] (match-value :open_round_bracket "(" remaining)]
      (when-let [[init remaining] (parse-expr remaining)]
        (when-let [[_ remaining] (match-value :semicolon ";" remaining)]
          (when-let [[condition remaining] (parse-expr remaining)]
            (when-let [[_ remaining] (match-value :semicolon ";" remaining)]
              (when-let [[update remaining] (parse-expr remaining)]
                (when-let [[_ remaining] (match-value :close_round_bracket ")" remaining)]
                  (when-let [[body remaining] (parse-statement remaining)]
                    [(->For init condition update body) remaining]))))))))))

(defn parse-statement
  "Разбирает любой оператор.
   Порядок правил определяет приоритет разбора.
   
   ## Параметры
   - `tokens` - последовательность токенов
   
   ## Возвращает
   Результат первого успешного разбора из:
   - условный оператор if
   - цикл while
   - цикл for
   - оператор return
   - блок кода
   - выражение
   
   ## Пример
   ```clojure
   (parse-statement [[:if_keyword \"if\"] [:open_round_bracket \"(\"] 
                     [:number \"1\"] [:close_round_bracket \")\"] 
                     [:open_curly_bracket \"{\"] [:close_curly_bracket \"}\"]])
   ;; => [(->If (->Num 1) (->Block []) nil) []]
   ```"
  [tokens]
  (or (parse-if tokens)
      (parse-while tokens)
      (parse-for tokens)
      (parse-return tokens)
      (parse-block tokens)
      (parse-expr tokens)))

;; =============================================
;; Основная функция парсинга
;; =============================================

(defn parse
  "Разбирает входную строку в AST.
   
   ## Параметры
   - `input` - строка с исходным кодом
   
   ## Возвращает
   AST или `nil`, если разбор не удался.
   
   ## Пример
   ```clojure
   (parse \"if (x > 0) { return 1; } else { return 0; }\")
   ;; => (->If (->BinaryOp \">\" (->Variable \"x\") (->Num 0))
   ;;          (->Block [(->Return (->Num 1))])
   ;;          (->Block [(->Return (->Num 0))]))
   ```"
  [input]
  (let [tokens (tokenize input)
        [expr remaining] (parse-statement tokens)]
    (when (empty? remaining)  ; Проверяем, что разобрали все токены
      expr)))
    