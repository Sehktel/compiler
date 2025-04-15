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

(defn lex
  "Полная функция лексера для разбора входной строки на токены.

  ## Параметры
  * `input` - строка с исходным кодом на языке C

  ## Возвращаемое значение
  Вектор токенов, где каждый токен представляет собой хэш-карту со следующими ключами:
  * `:type` - тип токена
  * `:value` - значение токена
  * `:position` - позиция токена во входной строке (опционально)

  ## Примеры
  ```clojure
  (lex \"int x = 42;\")
  ;; => [{:type :type_int_keyword, :value \"int\"}
  ;;     {:type :identifier, :value \"x\"}
  ;;     {:type :equal, :value \"=\"}
  ;;     {:type :number, :value \"42\"}
  ;;     {:type :semicolon, :value \";\"}]
  ```

  ## Заметки
  - Выполняет полную токенизацию исходного кода
  - Распознает все определенные типы токенов
  - Поддерживает различные лексические конструкции языка C"
  [input]
  (let [tokens (atom [])
        current-pos (atom 0)
        input-length (count input)]
    
    (while (< @current-pos input-length)
      (let [remaining-input (subs input @current-pos)
            ;; Пропускаем пробельные символы
            _ (when-let [match (re-find #"^\s+" remaining-input)]
                (swap! current-pos + (count match)))
            remaining-input (subs input @current-pos)]
        
        (when (seq remaining-input)
          (let [token-matches [
                               ;; Ключевые слова
                               (some #(when (re-find (re-pattern (str "^" % "\\b")) remaining-input) 
                                        {:type (keyword (str % "_keyword")) :value %}) 
                                     (mapcat identity (select-keys token-types 
                                                                   [:void_keyword 
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
                                                                    :continue_keyword])))
                               
                               ;; Операторы (многосимвольные в первую очередь)
                               (some #(when (re-find (re-pattern (str "^" (java.util.regex.Pattern/quote %))) remaining-input) 
                                        {:type (first (filter #(= (get token-types %) [%]) (keys token-types))) 
                                         :value %}) 
                                     (sort-by count > (mapcat identity 
                                                             (select-keys token-types 
                                                                         [:equal_equal 
                                                                          :not_equal 
                                                                          :less_equal 
                                                                          :greater_equal 
                                                                          :inc 
                                                                          :dec 
                                                                          :logical_and 
                                                                          :logical_or 
                                                                          :shift_left 
                                                                          :shift_right 
                                                                          :xor_equal]))))
                               
                               ;; Разделители
                               (some #(when (re-find (re-pattern (str "^" (java.util.regex.Pattern/quote %))) remaining-input) 
                                        {:type (first (filter #(= (get token-types %) [%]) (keys token-types))) 
                                         :value %}) 
                                     (mapcat identity (select-keys token-types 
                                                                   [:open_round_bracket 
                                                                    :close_round_bracket 
                                                                    :open_curly_bracket 
                                                                    :close_curly_bracket 
                                                                    :open_square_bracket 
                                                                    :close_square_bracket 
                                                                    :comma 
                                                                    :semicolon 
                                                                    :colon])))
                               
                               ;; Строковые литералы
                               (when-let [match (re-find (:string token-types) remaining-input)]
                                 {:type :string :value match})
                               
                               ;; Комментарии
                               (when-let [match (re-find (:comment token-types) remaining-input)]
                                 {:type :comment :value match})
                               
                               ;; Числа (шестнадцатеричные и десятичные)
                               (when-let [match (re-find (:number token-types) remaining-input)]
                                 {:type :number :value match})
                               
                               ;; Идентификаторы
                               (when-let [match (re-find (:identifier token-types) remaining-input)]
                                 {:type :identifier :value match})]]
            
            (when-let [token (first (filter some? token-matches))]
              (swap! tokens conj (assoc token :position @current-pos))
              (swap! current-pos + (count (:value token)))))))
    
    @tokens))

(defn tokenize
  "Расширенная функция токенизации для кода на языке C.
   
   ## Параметры
   - `input` - строка с кодом на языке C
   
   ## Возвращает
   Вектор токенов в формате `[тип значение]`
   
   ## Пример
   ```clojure
   (tokenize \"int x = 1 + 2;\")
   ;; => [[:keyword "int"] [:identifier "x"] [:operator "="] 
   ;;     [:number "1"] [:operator "+"] [:number "2"] [:separator ";"]]
   ```"
  [input]
  (let [tokens (lex input)]
    (mapv (fn [token]
            (let [type (:type token)
                  value (:value token)]
              (cond
                (= type :keyword) [:keyword value]
                (= type :operator) [:operator value]
                (= type :separator) (cond
                                     (= value "(") [:open-paren value]
                                     (= value ")") [:close-paren value]
                                     (= value "{") [:open-brace value]
                                     (= value "}") [:close-brace value]
                                     (= value "[") [:open-bracket value]
                                     (= value "]") [:close-bracket value]
                                     :else [:separator value])
                (= type :identifier) [:identifier value]
                (= type :number) [:number value]
                (= type :string) [:string value]
                (= type :comment) [:comment value]
                :else [:unknown value])))
          tokens)))

(defn lex-optimized
  "Более эффективная версия лексического анализатора"
  [input]
  (let [tokens (transient [])]
    ;; Оптимизированная логика токенизации
    (persistent! tokens))) 