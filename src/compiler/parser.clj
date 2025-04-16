(ns compiler.parser
  (:require [clojure.string :as str]
            [compiler.lexer :refer [tokenize]]
            [compiler.ast :refer [->Num ->BinaryOp ->Parens ->Variable ->FunctionCall 
                                 ->If ->While ->For ->Return ->UnaryOp ->Block
                                 ->SfrDeclaration ->SbitDeclaration ->InterruptFunction]]))

;; Флаг для включения/отключения режима отладки
(def ^:dynamic *debug-mode* true)

;; Предварительное объявление всех функций
(declare parse-if)
(declare parse-unary-op)
(declare parse-term)
(declare parse-expr)
(declare parse-statement)
(declare parse)
;; Добавляем новые объявления функций для 8051
(declare parse-sfr-declaration)
(declare parse-sbit-declaration)
(declare parse-interrupt-function)

;; Функция для логирования отладочной информации
(defn debug-log [& args]
  (when *debug-mode*
    (apply println args)))

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
      (debug-log "Токен соответствует:" token-type token-value)
      [token-value (rest tokens)])
    (do
      (debug-log "Токен не соответствует предикату:" (first tokens))
      nil)))

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
  (when (and (seq tokens) (= (first (first tokens)) :number))
    [(->Num (read-string (second (first tokens)))) (rest tokens)]))

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
   (parse-parens [[:open-paren \"(\"] [:number \"42\"] [:close-paren \")\"]])
   ;; => [(->Parens (->Num 42)) []]
   ```"
  [tokens]
  ;; Проверяем, соответствует ли первый токен открывающей скобке
  (when (and (seq tokens) (= (first (first tokens)) :open-paren))
    ;; Если да, разбираем выражение в скобках
    (let [remaining (rest tokens)]
      (when-let [[expr remaining] (parse-expr remaining)]
        ;; Проверяем, соответствует ли следующий токен закрывающей скобке
        (when (and (seq remaining) (= (first (first remaining)) :close-paren))
          [(->Parens expr) (rest remaining)])))))

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
   (parse-binary-op [[:number \"1\"] [:operator \"+\"] [:number \"2\"]])
   ;; => [(->BinaryOp \"+\" (->Num 1) (->Num 2)) []]
   ```"
  [tokens]
  (if (< (count tokens) 3)
    nil
    (when-let [[left remaining] (parse-term tokens)]
      (when (and (seq remaining) (= (first (first remaining)) :operator))
        (let [op (second (first remaining))
              remaining2 (rest remaining)]
          (when-let [[right remaining3] (parse-term remaining2)]
            [(->BinaryOp op left right) remaining3]))))))

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
  "Разбирает терм (число, переменную, выражение в скобках, и т.д.).
   
   ## Параметры
   - `tokens` - последовательность токенов
   
   ## Возвращает
   Вектор `[терм оставшиеся_токены]` или `nil` в случае ошибки."
  [tokens]
  (debug-log "Парсинг терма. Токены:" tokens)
  (or
   ;; Сначала пробуем разобрать число
   (when-let [res (parse-number tokens)]
     (debug-log "Успешный парсинг числа:" (first res))
     res)
   
   ;; Затем пробуем разобрать переменную
   (when-let [res (parse-variable tokens)]
     (debug-log "Успешный парсинг переменной:" (first res))
     res)
   
   ;; Затем пробуем разобрать выражение в скобках
   (when-let [res (parse-parens tokens)]
     (debug-log "Успешный парсинг выражения в скобках:" (first res))
     res)
   
   ;; Если ни один из вариантов не сработал, возвращаем nil
   (do
     (debug-log "Ошибка при парсинге терма. Токены не соответствуют ни одному паттерну.")
     nil)))

;; Определяем приоритеты операторов
(def operator-precedence
  {"*" 2
   "/" 2
   "+" 1
   "-" 1})

(defn parse-expr
  "Разбирает выражение (терм или бинарная операция).
   
   ## Параметры
   - `tokens` - последовательность токенов
   
   ## Возвращает
   Вектор `[выражение оставшиеся_токены]` или `nil` в случае ошибки."
  [tokens]
  (debug-log "Парсинг выражения. Токены:" tokens)
  (if (empty? tokens)
    (do
      (debug-log "Ошибка: Пустой список токенов при парсинге выражения")
      nil)
    (or
     ;; Сначала пробуем разобрать бинарную операцию
     (when-let [res (parse-binary-op tokens)]
       (debug-log "Успешный парсинг бинарной операции:" (first res))
       res)
     
     ;; Затем пробуем разобрать просто терм
     (when-let [res (parse-term tokens)]
       (debug-log "Успешный парсинг терма как выражения:" (first res))
       res)
     
     ;; Если ни один из вариантов не сработал, возвращаем nil
     (do
       (debug-log "Ошибка при парсинге выражения. Токены не соответствуют ни одному паттерну.")
       nil))))

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
  "Разбирает оператор или блок операторов.
   
   ## Параметры
   - `tokens` - последовательность токенов
   
   ## Возвращает
   Вектор `[оператор оставшиеся_токены]` или `nil` в случае ошибки."
  [tokens]
  (debug-log "Парсинг оператора. Токены:" tokens)
  (or
   ;; Сначала пробуем разобрать блок кода
   (when-let [res (parse-block tokens)]
     (debug-log "Успешный парсинг блока")
     res)
   
   ;; Пробуем разобрать оператор if
   (when-let [res (parse-if tokens)]
     (debug-log "Успешный парсинг оператора if")
     res)
   
   ;; Пробуем разобрать объявление SFR (8051)
   (when-let [res (parse-sfr-declaration tokens)]
     (debug-log "Успешный парсинг объявления SFR")
     res)
   
   ;; Пробуем разобрать объявление SBIT (8051)
   (when-let [res (parse-sbit-declaration tokens)]
     (debug-log "Успешный парсинг объявления SBIT")
     res)
   
   ;; Пробуем разобрать функцию с прерыванием (8051)
   (when-let [res (parse-interrupt-function tokens)]
     (debug-log "Успешный парсинг функции с прерыванием")
     res)
   
   ;; Остальные случаи
   (parse-while tokens)
   (parse-for tokens)
   (parse-return tokens)
   (parse-expr tokens)))

;; =============================================
;; Специальные функции для микроконтроллера 8051
;; =============================================

(defn parse-sfr-declaration
  "Разбирает объявление специального функционального регистра (SFR).
   
   ## Параметры
   - `tokens` - последовательность токенов
   
   ## Возвращает
   Вектор `[(->SfrDeclaration имя значение) оставшиеся_токены]` или `nil`
   
   ## Пример
   ```c
   sfr SP = 0x81;
   ```"
  [tokens]
  (debug-log "Парсинг объявления SFR. Токены:" tokens)
  (when-let [[_ remaining] (match-type :sfr_keyword tokens)]
    (when-let [[name remaining] (match-type :identifier remaining)]
      (when-let [[_ remaining] (match-type :equal remaining)]
        (when-let [[value remaining] (match-type :number remaining)]
          (when-let [[_ remaining] (match-type :semicolon remaining)]
            (debug-log "Успешный парсинг SFR:" name value)
            [(->SfrDeclaration name value) remaining]))))))

(defn parse-sbit-declaration
  "Разбирает объявление специального бита (SBIT).
   
   ## Параметры
   - `tokens` - последовательность токенов
   
   ## Возвращает
   Вектор `[(->SbitDeclaration имя значение) оставшиеся_токены]` или `nil`
   
   ## Пример
   ```c
   sbit P1_0 = 0x90;
   ```"
  [tokens]
  (debug-log "Парсинг объявления SBIT. Токены:" tokens)
  (when-let [[_ remaining] (match-type :sbit_keyword tokens)]
    (when-let [[name remaining] (match-type :identifier remaining)]
      (when-let [[_ remaining] (match-type :equal remaining)]
        (when-let [[value remaining] (match-type :number remaining)]
          (when-let [[_ remaining] (match-type :semicolon remaining)]
            (debug-log "Успешный парсинг SBIT:" name value)
            [(->SbitDeclaration name value) remaining]))))))

(defn parse-interrupt-function
  "Разбирает объявление функции с атрибутом interrupt.
   
   ## Параметры
   - `tokens` - последовательность токенов
   
   ## Возвращает
   Вектор `[(->InterruptFunction return-type name params vector-id body) оставшиеся_токены]` или `nil`
   
   ## Пример
   ```c
   void P14 (void) interrupt IE0_VECTOR { ... }
   ```"
  [tokens]
  (debug-log "Парсинг функции с прерыванием. Токены:" tokens)
  (when-let [[return-type remaining] (match-type :void_keyword tokens)]
    (when-let [[name remaining] (match-type :identifier remaining)]
      (when-let [[_ remaining] (match-type :open_round_bracket remaining)]
        (when-let [[_ remaining] (match-type :void_keyword remaining)]
          (when-let [[_ remaining] (match-type :close_round_bracket remaining)]
            (when-let [[_ remaining] (match-type :interrupt_keyword remaining)]
              (when-let [[vector-id remaining] (match-type :identifier remaining)]
                (when-let [[body-block remaining] (parse-statement remaining)]
                  (debug-log "Успешный парсинг функции с прерыванием:" name vector-id)
                  [(->InterruptFunction return-type name [] vector-id body-block) remaining])))))))))

;; =============================================
;; Основная функция парсинга
;; =============================================

(defn parse-complex-expr
  "Разбирает сложное выражение с несколькими операторами.
   
   ## Параметры
   - `tokens` - последовательность токенов
   
   ## Возвращает
   AST или `nil`, если разбор не удался."
  [tokens]
  (try
    (loop [remaining-tokens tokens
           expr-stack []
           op-stack []]
      (if (empty? remaining-tokens)
        ;; Если токены закончились, применяем оставшиеся операторы
        (if (empty? op-stack)
          ;; Если нет операторов, возвращаем последнее выражение
          (if (empty? expr-stack)
            nil  ;; Если стек выражений пуст, возвращаем nil
            (first expr-stack))
          ;; Иначе применяем операторы в порядке приоритета
          (if (or (empty? expr-stack) (< (count expr-stack) 2))
            nil  ;; Недостаточно операндов для оператора
            (let [op (peek op-stack)
                  right (peek expr-stack)
                  expr-stack' (pop expr-stack)
                  left (peek expr-stack')
                  expr-stack'' (pop expr-stack')
                  new-expr (->BinaryOp op left right)]
              (recur remaining-tokens (conj expr-stack'' new-expr) (pop op-stack)))))
        
        ;; Обработка текущего токена
        (let [token (first remaining-tokens)]
          (cond
            ;; Число
            (= (first token) :number)
            (let [value (read-string (second token))]
              (recur (rest remaining-tokens) (conj expr-stack (->Num value)) op-stack))
            
            ;; Открывающая скобка
            (= (first token) :open-paren)
            (let [end-paren-idx (.indexOf (mapv first remaining-tokens) :close-paren)]
              (if (neg? end-paren-idx)
                ;; Если нет закрывающей скобки, возвращаем nil
                nil
                (let [subexpr-tokens (subvec remaining-tokens 1 end-paren-idx)
                      subexpr (parse-complex-expr subexpr-tokens)]
                  (if (nil? subexpr)
                    nil
                    (recur (subvec remaining-tokens (inc end-paren-idx))
                           (conj expr-stack (->Parens subexpr))
                           op-stack)))))
            
            ;; Оператор
            (= (first token) :operator)
            (let [new-op (second token)
                  new-precedence (get operator-precedence new-op 0)]
              ;; Если есть оператор с большим приоритетом в стеке, применяем его
              (if (and (not (empty? op-stack))
                       (>= (get operator-precedence (peek op-stack) 0) new-precedence))
                (if (< (count expr-stack) 2)
                  nil  ;; Недостаточно операндов для оператора
                  (let [op (peek op-stack)
                        right (peek expr-stack)
                        expr-stack' (pop expr-stack)
                        left (peek expr-stack')
                        expr-stack'' (pop expr-stack')
                        new-expr (->BinaryOp op left right)]
                    (recur remaining-tokens (conj expr-stack'' new-expr) (pop op-stack))))
                ;; Иначе добавляем оператор в стек
                (recur (rest remaining-tokens) expr-stack (conj op-stack new-op))))
            
            ;; Если токен не распознан, возвращаем nil
            :else nil))))
    ;; Обработка любых исключений
    (catch Exception e
      nil)))

(defn parse
  "Главная функция синтаксического анализа.
   
   ## Параметры
   - `code` - исходный код программы или токены
   
   ## Возвращает
   Абстрактное синтаксическое дерево (AST) программы или `nil` в случае ошибки."
  [code]
  (let [tokens (if (string? code) (tokenize code) code)]
    (debug-log "Начало парсинга. Токены:" tokens)
    (loop [remaining tokens
           statements []]
      (if (empty? remaining)
        ;; Все токены обработаны, возвращаем результат
        {:type :program
         :body statements}
        
        ;; Пробуем разобрать следующий оператор
        (let [result (or (parse-statement remaining)
                        (parse-sfr-declaration remaining)  ;; Проверяем 8051-специфичные объявления
                        (parse-sbit-declaration remaining)
                        (parse-interrupt-function remaining))]
          (if result
            (let [[stmt new-remaining] result]
              (recur new-remaining (conj statements stmt)))
            ;; Ошибка синтаксиса
            (do
              (debug-log "Ошибка синтаксиса. Токены:" remaining)
              nil))))))
    