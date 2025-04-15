(ns compiler.lexer
  (:require [clojure.string :as str]))

;; Предварительное объявление всех функций
(declare is-keyword?)
(declare operator?)
(declare separator?)
(declare get-token-type)
(declare lex)
(declare tokenize)

;; =============================================
;; Определение типов токенов
;; =============================================

(def token-types
  "Словарь, определяющий все возможные типы токенов и их значения.
   
   ## Структура
   Каждый ключ - это тип токена, значение - список возможных значений или регулярное выражение.
   
   ## Типы токенов
   - Ключевые слова языка C (void, if, else и т.д.)
   - Скобки и разделители ((), {}, [], ,, ;, :)
   - Операторы (+, -, *, /, ==, != и т.д.)
   - Идентификаторы (имена переменных и функций)
   - Числовые литералы (десятичные и шестнадцатеричные)
   - Строковые литералы
   - Комментарии (однострочные и многострочные)"
  {:void_keyword ["void"]
   :interrupt_keyword ["interrupt"]
   :type_char_keyword ["char"]
   :type_int_keyword ["int"]
   :signed_keyword ["signed"]
   :unsigned_keyword ["unsigned"]
   :const_keyword ["const"]
   :volatile_keyword ["volatile"]
   :typedef_keyword ["typedef"]
   :goto_keyword ["goto"]
   :if_keyword ["if"]
   :else_keyword ["else"]
   :for_keyword ["for"]
   :while_keyword ["while"]
   :do_keyword ["do"]
   :return_keyword ["return"]
   :break_keyword ["break"]
   :continue_keyword ["continue"]

   :open_round_bracket ["("]
   :close_round_bracket [")"]
   :open_curly_bracket ["{"]
   :close_curly_bracket ["}"]
   :open_square_bracket ["["]
   :close_square_bracket ["]"]
   :comma [","]
   :semicolon [";"]
   :colon [":"]
   
   :plus ["+"]
   :minus ["-"]
   :asterisk ["*"]
   :slash ["/"]
   :percent ["%"]
   :equal ["="]
   :equal_equal ["=="]
   :not_equal ["!="]
   :xor_equal ["^="]
   :less ["<"]
   :less_equal ["<="]
   :greater [">"]
   :greater_equal [">="]
   :inc ["++"]
   :dec ["--"]
   :logical_and ["&&"]
   :logical_or ["||"]
   :logical_not ["!"]
   :bit_and ["&"]
   :bit_or ["|"]
   :bit_xor ["^"]
   :bit_not ["~"]
   :shift_left ["<<"]
   :shift_right [">>"]
   
   :identifier #"[a-zA-Z_\p{L}][a-zA-Z0-9_\p{L}]*"
   :number #"0[xX][0-9a-fA-F]+|\d+"
   :string #"\"[^\"]*\""
   :comment #"//[^\n]*|/\*[\s\S]*?\*/"})

(defn is-keyword?
  "Проверяет, является ли строка ключевым словом языка C.
   
   ## Параметры
   - `s` - строка для проверки
   
   ## Возвращает
   `true`, если строка является ключевым словом, иначе `false`.
   
   ## Пример
   ```clojure
   (is-keyword? \"if\")  ;; => true
   (is-keyword? \"xyz\") ;; => false
   ```"
;; TODO: define keywords ifndef endif define undef
  [s]
  (some #(some (partial = s) %) 
        (vals (select-keys token-types 
                          [ :void_keyword
                            :interrupt_keyword
                            :type_char_keyword
                            :type_int_keyword
                            :signed_keyword
                            :unsigned_keyword
                            :const_keyword
                            :volatile_keyword
                            :typedef_keyword
                            :goto_keyword
                            :if_keyword
                            :else_keyword
                            :for_keyword
                            :while_keyword
                            :do_keyword
                            :return_keyword
                            :break_keyword
                            :continue_keyword]))))

(defn operator?
  "Проверяет, является ли строка оператором языка C.
   
   ## Параметры
   - `s` - строка для проверки
   
   ## Возвращает
   `true`, если строка является оператором, иначе `false`.
   
   ## Пример
   ```clojure
   (operator? \"+\")  ;; => true
   (operator? \"xyz\") ;; => false
   ```"
  [s]
  (some #(some (partial = s) %) 
        (vals (select-keys token-types 
                          [ :plus 
                            :minus 
                            :asterisk 
                            :slash 
                            :percent 
                            :equal 
                            :equal_equal 
                            :not_equal 
                            :xor_equal 
                            :less 
                            :less_equal 
                            :greater 
                            :greater_equal 
                            :inc 
                            :dec 
                            :logical_and 
                            :logical_or 
                            :logical_not 
                            :bit_and 
                            :bit_or 
                            :bit_xor 
                            :bit_not 
                            :shift_left 
                            :shift_right]))))

(defn separator?
  "Проверяет, является ли строка разделителем языка C.
   
   ## Параметры
   - `s` - строка для проверки
   
   ## Возвращает
   `true`, если строка является разделителем, иначе `false`.
   
   ## Пример
   ```clojure
   (separator? \";\")  ;; => true
   (separator? \"xyz\") ;; => false
   ```"
  [s]
  (some #(some (partial = s) %) 
        (vals (select-keys token-types 
                          [:open_round_bracket 
                           :close_round_bracket 
                           :open_curly_bracket
                           :close_curly_bracket 
                           :open_square_bracket 
                           :close_square_bracket 
                           :comma 
                           :semicolon 
                           :colon]))))

(defn get-token-type
  "Определяет тип токена на основе его значения.
   
   ## Параметры
   - `token` - строка, представляющая токен
   
   ## Возвращает
   Ключевое слово, обозначающее тип токена:
   - `:keyword` - ключевое слово языка
   - `:operator` - оператор
   - `:separator` - разделитель
   - `:identifier` - идентификатор
   - `:number` - числовой литерал
   - `:string` - строковый литерал
   - `:comment` - комментарий
   - `:unknown` - неизвестный тип
   
   ## Пример
   ```clojure
   (get-token-type \"if\")    ;; => :keyword
   (get-token-type \"+\")     ;; => :operator
   (get-token-type \"x\")     ;; => :identifier
   (get-token-type \"42\")    ;; => :number
   (get-token-type \"// comment\") ;; => :comment
   (get-token-type \"/* comment */\") ;; => :comment
   (get-token-type \"\"\" comment \"\"\") ;; => :string
   ```"
  [token]
  (cond
    (is-keyword? token) :keyword
    (operator? token) :operator
    (separator? token) :separator
    (re-matches (:identifier token-types) token) :identifier
    (re-matches (:number token-types) token) :number
    (re-matches (:string token-types) token) :string
    (re-matches (:comment token-types) token) :comment
    :else :unknown))

(defn find-exact-token-type
  "Определяет конкретный тип токена на основе его значения.
   
   ## Параметры
   - `value` - строка, представляющая токен
   
   ## Возвращает
   Конкретный ключ из map token-types или :identifier если не найден
   конкретный тип."
  [value]
  (or 
   (first 
    (filter 
     #(and (not (#{:identifier :number :string :comment} %))
           (let [token-val (get token-types %)]
             (if (vector? token-val)
               (some (partial = value) token-val)
               false)))
     (keys token-types)))
   :identifier))

(defn lex
  "Полноценный лексический анализатор с использованием конечного автомата (DFA).
   
   ## Основные принципы
   - Использование DFA для перехода между состояниями обработки токенов
   - Обработка всех типов токенов: ключевые слова, операторы, разделители, 
     идентификаторы, числовые и строковые литералы, комментарии
   - Позиционная информация для каждого токена
   - Обработка ошибок и неизвестных символов

   ## Параметры
   * `input` - исходный код на языке C

   ## Возвращает
   Вектор токенов в формате {:type :тип, :value \"значение\", :position позиция, :line строка, :column колонка}"
  [input]
  (let [tokens (transient [])
        lines (str/split-lines input)
        input-with-newlines (str/join "\n" lines)]
    
    (loop [pos 0
           line 1
           col 1
           state :initial]
      (if (>= pos (count input-with-newlines))
        ;; Достигнут конец входной строки
        (persistent! tokens)
        
        (let [current-char (get input-with-newlines pos)]
          (case state
            ;; Начальное состояние - определение типа токена
            :initial
            (cond
              ;; Пропуск пробельных символов
              (Character/isWhitespace current-char)
              (let [new-line (if (= current-char \newline) (inc line) line)
                    new-col (if (= current-char \newline) 1 (inc col))]
                (recur (inc pos) new-line new-col :initial))
              
              ;; Идентификаторы и ключевые слова
              (or (Character/isLetter current-char) 
                  (= current-char \_))
              (recur pos line col :identifier)
              
              ;; Числовые литералы
              (Character/isDigit current-char)
              (recur pos line col :number)
              
              ;; Строковые литералы
              (= current-char \")
              (recur pos line col :string)
              
              ;; Комментарии
              (= current-char \/)
              (if (< (inc pos) (count input-with-newlines))
                (let [next-char (get input-with-newlines (inc pos))]
                  (cond
                    ;; Однострочный комментарий //
                    (= next-char \/)
                    (recur pos line col :single-line-comment)
                    
                    ;; Многострочный комментарий /*
                    (= next-char \*)
                    (recur pos line col :multi-line-comment)
                    
                    ;; Просто оператор деления
                    :else
                    (recur pos line col :operator)))
                ;; Если / - последний символ, то это оператор
                (recur pos line col :operator))
              
              ;; Операторы и потенциально составные операторы
              (#{\+ \- \* \% \= \! \< \> \& \| \^ \~} current-char)
              (recur pos line col :operator)
              
              ;; Разделители
              (#{\( \) \{ \} \[ \] \; \, \:} current-char)
              (let [token-value (str current-char)
                    token-type (cond 
                                 (= current-char \() :open_round_bracket
                                 (= current-char \)) :close_round_bracket
                                 (= current-char \{) :open_curly_bracket
                                 (= current-char \}) :close_curly_bracket
                                 (= current-char \[) :open_square_bracket
                                 (= current-char \]) :close_square_bracket
                                 (= current-char \;) :semicolon
                                 (= current-char \,) :comma
                                 (= current-char \:) :colon)]
                (conj! tokens {:type token-type
                               :value token-value
                               :position pos
                               :line line
                               :column col})
                (recur (inc pos) line (inc col) :initial))
              
              ;; Неизвестные символы
              :else
              (do 
                (println (format "Неизвестный символ: %c на позиции %d (строка %d, колонка %d)" 
                                current-char pos line col))
                (recur (inc pos) line (inc col) :initial)))
            
            ;; Состояние обработки идентификатора
            :identifier
            (let [start-pos pos
                  [value end-pos] (loop [end pos]
                                    (if (and (< end (count input-with-newlines))
                                             (or (Character/isLetterOrDigit (get input-with-newlines end))
                                                 (= (get input-with-newlines end) \_)))
                                      (recur (inc end))
                                      [(subs input-with-newlines start-pos end) end]))
                  token-type (if (is-keyword? value)
                              (find-exact-token-type value)
                              :identifier)]
              (conj! tokens {:type token-type
                             :value value
                             :position start-pos
                             :line line
                             :column col})
              (recur end-pos line (+ col (- end-pos start-pos)) :initial))
            
            ;; Состояние обработки числового литерала
            :number
            (let [start-pos pos
                  [value end-pos] (if (and (= (get input-with-newlines pos) \0)
                                           (< (inc pos) (count input-with-newlines))
                                           (#{\x \X} (get input-with-newlines (inc pos))))
                                    ;; Шестнадцатеричное число
                                    (loop [end (+ pos 2)]
                                      (if (and (< end (count input-with-newlines))
                                               (Character/digit (get input-with-newlines end) 16) >= 0)
                                        (recur (inc end))
                                        [(subs input-with-newlines start-pos end) end]))
                                    ;; Десятичное число
                                    (loop [end pos]
                                      (if (and (< end (count input-with-newlines))
                                               (Character/isDigit (get input-with-newlines end)))
                                        (recur (inc end))
                                        [(subs input-with-newlines start-pos end) end])))]
              (conj! tokens {:type :number
                             :value value
                             :position start-pos
                             :line line
                             :column col})
              (recur end-pos line (+ col (- end-pos start-pos)) :initial))
            
            ;; Состояние обработки строкового литерала
            :string
            (let [start-pos pos
                  [value end-pos] (loop [end (inc pos) ; пропускаем открывающую кавычку
                                         escaped false]
                                    (cond
                                      ;; Достигнут конец текста без закрывающей кавычки
                                      (>= end (count input-with-newlines))
                                      [(str (subs input-with-newlines start-pos end) "\"") (inc end)]
                                      
                                      ;; Нашли закрывающую кавычку (не экранированную)
                                      (and (= (get input-with-newlines end) \")
                                           (not escaped))
                                      [(subs input-with-newlines start-pos (inc end)) (inc end)]
                                      
                                      ;; Нашли экранирующий символ
                                      (and (= (get input-with-newlines end) \\)
                                           (not escaped))
                                      (recur (inc end) true)
                                      
                                      ;; Обрабатываем экранированный символ
                                      escaped
                                      (recur (inc end) false)
                                      
                                      ;; Обычный символ
                                      :else
                                      (recur (inc end) false)))]
              (conj! tokens {:type :string
                             :value value
                             :position start-pos
                             :line line
                             :column col})
              ;; Подсчитываем количество новых строк в строковом литерале для корректировки line и col
              (let [newlines (count (filter #(= % \newline) value))
                    last-nl-pos (if-let [pos (str/last-index-of value "\n")] 
                                  pos 
                                  0)]
                (if (pos? newlines)
                  (let [new-col (- (count value) last-nl-pos)]
                    (recur end-pos (+ line newlines) new-col :initial))
                  (recur end-pos line (+ col (count value)) :initial))))
            
            ;; Состояние обработки однострочного комментария
            :single-line-comment
            (let [start-pos pos
                  [value end-pos] (loop [end pos]
                                    (if (or (>= end (count input-with-newlines))
                                            (= (get input-with-newlines end) \newline))
                                      [(subs input-with-newlines start-pos end) end]
                                      (recur (inc end))))]
              (conj! tokens {:type :comment
                             :value value
                             :position start-pos
                             :line line
                             :column col})
              (recur end-pos line (+ col (count value)) :initial))
            
            ;; Состояние обработки многострочного комментария
            :multi-line-comment
            (let [start-pos pos
                  [value end-pos newlines last-nl-pos] 
                  (loop [end pos
                         nl-count 0
                         last-nl end
                         inside-escape false]
                    (if (>= (+ end 1) (count input-with-newlines))
                      [(subs input-with-newlines start-pos (inc end)) (inc end) nl-count last-nl]
                      (let [curr-char (get input-with-newlines end)
                            next-char (get input-with-newlines (inc end))]
                        (cond
                          ;; Нашли конец комментария
                          (and (= curr-char \*) (= next-char \/) (not inside-escape))
                          [(subs input-with-newlines start-pos (+ end 2)) (+ end 2) nl-count last-nl]
                          
                          ;; Экранирование символа
                          (and (= curr-char \\) (not inside-escape))
                          (recur (inc end) nl-count last-nl true)
                          
                          ;; После экранирования - продолжаем обычный режим
                          inside-escape
                          (recur (inc end) nl-count last-nl false)
                          
                          ;; Новая строка в комментарии
                          (= curr-char \newline)
                          (recur (inc end) (inc nl-count) end false)
                          
                          ;; Обычный символ комментария
                          :else
                          (recur (inc end) nl-count last-nl false)))))]
              (conj! tokens {:type :comment
                             :value value
                             :position start-pos
                             :line line
                             :column col})
              (if (pos? newlines)
                (let [new-col (- end-pos last-nl-pos)]
                  (recur end-pos (+ line newlines) new-col :initial))
                (recur end-pos line (+ col (count value)) :initial)))
            
            ;; Состояние обработки операторов
            :operator
            (let [start-pos pos
                  current (get input-with-newlines pos)
                  next-pos (inc pos)
                  has-next (< next-pos (count input-with-newlines))
                  next-char (when has-next (get input-with-newlines next-pos))
                  
                  ;; Проверяем возможные двух-символьные операторы
                  two-char-op (when has-next
                                (str current next-char))
                  
                  ;; Проверяем, есть ли такой двух-символьный оператор
                  is-two-char-op (and has-next
                                     (some #(= two-char-op %) 
                                           (mapcat val (select-keys token-types 
                                                                   [:equal_equal :not_equal :less_equal
                                                                    :greater_equal :inc :dec :logical_and
                                                                    :logical_or :shift_left :shift_right
                                                                    :xor_equal]))))
                  
                  ;; Значение и тип токена в зависимости от того, одно- или двух-символьный оператор
                  [token-value end-pos] (if is-two-char-op
                                         [two-char-op (+ pos 2)]
                                         [(str current) (inc pos)])
                  
                  token-type (cond
                               (= token-value "+") :plus
                               (= token-value "-") :minus
                               (= token-value "*") :asterisk
                               (= token-value "/") :slash
                               (= token-value "%") :percent
                               (= token-value "=") :equal
                               (= token-value "==") :equal_equal
                               (= token-value "!=") :not_equal
                               (= token-value "^=") :xor_equal
                               (= token-value "<") :less
                               (= token-value "<=") :less_equal
                               (= token-value ">") :greater
                               (= token-value ">=") :greater_equal
                               (= token-value "++") :inc
                               (= token-value "--") :dec
                               (= token-value "&&") :logical_and
                               (= token-value "||") :logical_or
                               (= token-value "!") :logical_not
                               (= token-value "&") :bit_and
                               (= token-value "|") :bit_or
                               (= token-value "^") :bit_xor
                               (= token-value "~") :bit_not
                               (= token-value "<<") :shift_left
                               (= token-value ">>") :shift_right
                               :else :unknown)]
              
              (conj! tokens {:type token-type
                             :value token-value
                             :position start-pos
                             :line line
                             :column col})
              (recur end-pos line (+ col (- end-pos start-pos)) :initial))))))))

(defn tokenize
  "Расширенная функция токенизации для кода на языке C.
   
   ## Параметры
   - `input` - строка с кодом на языке C
   
   ## Возвращает
   Вектор токенов в формате `[тип значение]`
   
   ## Пример
   ```clojure
   (tokenize \"int x = 1 + 2;\")
   ;; => [[:keyword \"int\"] [:identifier \"x\"] [:operator \"=\"] 
   ;;     [:number \"1\"] [:operator \"+\"] [:number \"2\"] [:separator \";\"]]
   ```"
  [input]
  (let [tokens (lex input)]
    (mapv (fn [token]
            (let [type (:type token)
                  value (:value token)]
              (cond
                (#{:void_keyword :interrupt_keyword :type_char_keyword
                   :type_int_keyword :signed_keyword :unsigned_keyword
                   :const_keyword :volatile_keyword :typedef_keyword
                   :goto_keyword :if_keyword :else_keyword
                   :for_keyword :while_keyword :do_keyword
                   :return_keyword :break_keyword :continue_keyword} type) 
                [:keyword value]
                
                (#{:plus :minus :asterisk :slash :percent
                   :equal :equal_equal :not_equal :xor_equal
                   :less :less_equal :greater :greater_equal
                   :inc :dec :logical_and :logical_or :logical_not
                   :bit_and :bit_or :bit_xor :bit_not
                   :shift_left :shift_right} type) 
                [:operator value]
                
                ;; Разделители и скобки
                (= type :open_round_bracket) [:open-paren value]
                (= type :close_round_bracket) [:close-paren value]
                (= type :open_curly_bracket) [:open-brace value]
                (= type :close_curly_bracket) [:close-brace value]
                (= type :open_square_bracket) [:open-bracket value]
                (= type :close_square_bracket) [:close-bracket value]
                (#{:comma :semicolon :colon} type) [:separator value]
                
                (= type :identifier) [:identifier value]
                (= type :number) [:number value]
                (= type :string) [:string value]
                (= type :comment) [:comment value]
                :else [:unknown value])))
          tokens)))

;; Удаляем устаревшую заглушку оптимизированной версии
;; так как новая реализация уже оптимизирована

;; Дополнительные вспомогательные функции анализатора

(defn get-tokens-debug
  "Вспомогательная функция для отладки - возвращает подробную информацию о токенах.
   
   ## Параметры
   - `input` - строка с кодом на языке C
   
   ## Возвращает
   Вектор токенов с полной информацией (тип, значение, позиция, строка, колонка)"
  [input]
  (lex input))

(defn validate-tokens
  "Проверяет корректность токенов и выводит информацию об ошибках.
   
   ## Параметры
   - `tokens` - вектор токенов, полученный функцией lex
   
   ## Возвращает
   true, если все токены корректны, false если есть ошибки"
  [tokens]
  (let [unknown-tokens (filter #(= (:type %) :unknown) tokens)]
    (when (seq unknown-tokens)
      (doseq [token unknown-tokens]
        (println (format "Ошибка лексического анализа: неизвестный токен '%s' на позиции %d (строка %d, колонка %d)"
                         (:value token) (:position token) (:line token) (:column token)))))
    (empty? unknown-tokens)))

(defn tokenize-with-positions
  "Возвращает токены с сохранением позиционной информации.
   
   ## Параметры
   - `input` - строка с кодом на языке C
   
   ## Возвращает
   Вектор токенов в формате `{:type тип :value значение :position позиция :line строка :column колонка}`"
  [input]
  (let [tokens (lex input)]
    (mapv (fn [token]
            (let [type (:type token)
                  value (:value token)
                  general-type (cond
                                (#{:void_keyword :interrupt_keyword :type_char_keyword
                                   :type_int_keyword :signed_keyword :unsigned_keyword
                                   :const_keyword :volatile_keyword :typedef_keyword
                                   :goto_keyword :if_keyword :else_keyword
                                   :for_keyword :while_keyword :do_keyword
                                   :return_keyword :break_keyword :continue_keyword} type) 
                                :keyword
                                
                                (#{:plus :minus :asterisk :slash :percent
                                   :equal :equal_equal :not_equal :xor_equal
                                   :less :less_equal :greater :greater_equal
                                   :inc :dec :logical_and :logical_or :logical_not
                                   :bit_and :bit_or :bit_xor :bit_not
                                   :shift_left :shift_right} type) 
                                :operator
                                
                                ;; Разделители и скобки
                                (= type :open_round_bracket) :open-paren
                                (= type :close_round_bracket) :close-paren
                                (= type :open_curly_bracket) :open-brace
                                (= type :close_curly_bracket) :close-brace
                                (= type :open_square_bracket) :open-bracket
                                (= type :close_square_bracket) :close-bracket
                                (#{:comma :semicolon :colon} type) :separator
                                
                                :else type)]
              (assoc token :general-type general-type)))
          tokens)))

(defn filter-tokens
  "Фильтрует токены по заданному типу или типам.
   
   ## Параметры
   - `tokens` - список токенов
   - `types` - тип или список типов для фильтрации
   
   ## Возвращает
   Отфильтрованный список токенов"
  [tokens types]
  (let [type-set (if (set? types) types (set [types]))]
    (filter #(contains? type-set (:type %)) tokens)))

(defn strip-comments
  "Удаляет комментарии из списка токенов.
   
   ## Параметры
   - `tokens` - список токенов
   
   ## Возвращает
   Список токенов без комментариев"
  [tokens]
  (filter #(not= (:type %) :comment) tokens))

(defn token-seq->str
  "Преобразует последовательность токенов в строку.
   
   ## Параметры
   - `tokens` - последовательность токенов
   
   ## Возвращает
   Строковое представление токенов"
  [tokens]
  (str/join " " (map (fn [token]
                      (format "%s('%s')" 
                              (name (or (:general-type token) (:type token)))
                              (:value token)))
                    tokens)))

(defn lex-file
  "Выполняет лексический анализ содержимого файла.
   
   ## Параметры
   - `file-path` - путь к файлу
   
   ## Возвращает
   Список токенов, полученный после лексического анализа"
  [file-path]
  (try
    (let [content (slurp file-path)]
      (lex content))
    (catch Exception e
      (println (format "Ошибка при чтении файла %s: %s" file-path (.getMessage e)))
      [])))

(defn append-token-stream
  "Объединяет два потока токенов с корректировкой позиций.
   
   ## Параметры
   - `stream1` - первый поток токенов
   - `stream2` - второй поток токенов
   
   ## Возвращает
   Объединенный поток токенов с корректными позициями"
  [stream1 stream2]
  (if (empty? stream1)
    stream2
    (let [last-token (last stream1)
          last-pos (:position last-token)
          last-value (:value last-token)
          last-line (:line last-token)
          last-col (:column last-token)
          offset-pos (+ last-pos (count last-value))
          offset-col (+ last-col (count last-value))
          
          process-token (fn [token]
                         (let [pos (:position token)
                               first-pos (:position (first stream2))
                               rel-pos (- pos first-pos)
                               new-pos (+ offset-pos rel-pos)]
                          (if (zero? rel-pos)
                            ;; Первый токен второго потока
                            (assoc token 
                                  :position new-pos
                                  :line last-line
                                  :column offset-col)
                            ;; Остальные токены
                            (let [first-value (:value (first stream2))
                                  prefix (subs first-value 0 rel-pos)
                                  nl-count (count (filter #(= % \newline) prefix))]
                              (if (zero? nl-count)
                                (assoc token 
                                      :position new-pos
                                      :line last-line
                                      :column (+ offset-col rel-pos))
                                (let [last-nl (str/last-index-of prefix "\n")
                                      last-nl-pos (if last-nl last-nl 0)]
                                  (assoc token
                                        :position new-pos
                                        :line (+ last-line nl-count)
                                        :column (- rel-pos last-nl-pos))))))))]
      
      (vec (concat stream1 (mapv process-token stream2))))))

(defn -main 
  "Функция для демонстрации работы лексического анализатора.
   Выполняет анализ тестового кода и выводит результаты."
  [& args]
  (println "Демонстрация работы лексического анализатора")
  (println "--------------------------------------------")
  
  (let [test-code "int main() {
    // Это комментарий
    int x = 10;
    if (x > 5) {
        printf(\"Hello, world!\");
        return 0;
    }
    /* Многострочный 
       комментарий */
    return 1;
}"]
    (println "Исходный код:")
    (println test-code)
    (println "\nРезультат лексического анализа:")
    
    (let [tokens (lex test-code)]
      (doseq [token tokens]
        (printf "Тип: %-20s Значение: %-20s Строка: %d Колонка: %d\n" 
                (name (:type token)) 
                (:value token) 
                (:line token) 
                (:column token)))
      
      (println "\nВалидация токенов:")
      (println (if (validate-tokens tokens)
                 "Все токены корректны"
                 "Обнаружены ошибки в токенах"))
      
      (println "\nТокены в формате для парсера:")
      (println (token-seq->str (tokenize-with-positions test-code)))))) 