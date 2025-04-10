(ns compiler.lexer
  (:require [clojure.string :as str]))

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
   :type_keyword ["char" "int"]
   :signed_keyword ["signed"]
   :unsigned_keyword ["unsigned"]
   :const_keyword ["const"]
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
   
   :identifier #"[a-zA-Z_][a-zA-Z0-9_]*"
   :number #"0[xX][0-9a-fA-F]+|\d+"
   :string #"\"[^\"]*\""
   :comment #"//.*|/\*[\s\S]*?\*/"})

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
  [s]
  (some #(some (partial = s) %) 
        (vals (select-keys token-types 
                          [:void_keyword :interrupt_keyword :type_keyword 
                           :signed_keyword :unsigned_keyword :const_keyword 
                           :typedef_keyword :goto_keyword :if_keyword 
                           :else_keyword :for_keyword :while_keyword 
                           :do_keyword :return_keyword :break_keyword 
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
                          [:plus :minus :asterisk :slash :percent 
                           :equal :equal_equal :not_equal :xor_equal 
                           :less :less_equal :greater :greater_equal 
                           :inc :dec :logical_and :logical_or :logical_not 
                           :bit_and :bit_or :bit_xor :bit_not 
                           :shift_left :shift_right]))))

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
                          [:open_round_bracket :close_round_bracket 
                           :open_curly_bracket :close_curly_bracket 
                           :open_square_bracket :close_square_bracket 
                           :comma :semicolon :colon]))))

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
  "Основная функция лексера. Разбивает входную строку на токены.
   
   ## Параметры
   - `input` - строка с исходным кодом на языке C
   
   ## Возвращает
   Вектор токенов, где каждый токен - это хэш-мапа с ключами:
   - `:type` - тип токена
   - `:value` - значение токена
   
   ## Пример
   ```clojure
   (lex \"int x = 42;\")
   ;; => [{:type :keyword, :value \"int\"}
   ;;     {:type :identifier, :value \"x\"}
   ;;     {:type :operator, :value \"=\"}
   ;;     {:type :number, :value \"42\"}
   ;;     {:type :separator, :value \";\"}]
   ```"
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
          (let [token (or
                       ;; Проверяем комментарии
                       (re-find #"^//.*" remaining-input)
                       (re-find #"^/\*[\s\S]*?\*/" remaining-input)
                       ;; Проверяем строки
                       (re-find #"^\"[^\"]*\"" remaining-input)
                       ;; Проверяем числа
                       (re-find #"^0[xX][0-9a-fA-F]+" remaining-input)
                       (re-find #"^\d+" remaining-input)
                       ;; Проверяем операторы и разделители
                       (re-find #"^[+\-*/%=!&|^~<>]+" remaining-input)
                       (re-find #"^[(){}\[\],;:]" remaining-input)
                       ;; Проверяем идентификаторы
                       (re-find #"^[a-zA-Z_][a-zA-Z0-9_]*" remaining-input))]
            
            (when token
              (let [token-type (get-token-type token)]
                (swap! tokens conj {:type token-type :value token})
                (swap! current-pos + (count token))))))))
    
    @tokens))

(defn test-lexer
  "Тестовая функция для демонстрации работы лексера.
   Выводит в консоль типы и значения токенов для тестового кода.
   
   ## Пример вывода
   ```
   Тип: keyword    Значение: void
   Тип: identifier Значение: timer0_isr
   Тип: separator  Значение: (
   Тип: separator  Значение: )
   Тип: keyword    Значение: interrupt
   Тип: number     Значение: 2
   Тип: separator  Значение: {
   Тип: comment    Значение: // Простой обработчик прерывания
   Тип: identifier Значение: P1
   Тип: operator   Значение: ^=
   Тип: number     Значение: 0x01
   Тип: separator  Значение: ;
   Тип: comment    Значение: // Инвертируем бит
   Тип: separator  Значение: }
   ```"
  []
  (let [test-code "void timer0_isr() interrupt 2 {
    // Простой обработчик прерывания
    P1 ^= 0x01;  // Инвертируем бит
}"
        tokens (lex test-code)]
    (doseq [token tokens]
      (println (format "Тип: %-10s Значение: %s" 
                      (name (:type token)) 
                      (:value token))))))

(defn tokenize
  "Упрощенная функция токенизации для базовых арифметических выражений.
   
   ## Параметры
   - `input` - строка с арифметическим выражением
   
   ## Возвращает
   Вектор токенов в формате `[тип значение]`, где тип может быть:
   - `:number` - числовой литерал
   - `:operator` - оператор (+, -, *, /)
   - `:open-paren` - открывающая скобка
   - `:close-paren` - закрывающая скобка
   - `:unknown` - неизвестный токен
   
   ## Пример
   ```clojure
   (tokenize \"1 + 2 * (3 - 4)\")
   ;; => [[:number \"1\"] [:operator \"+\"] [:number \"2\"]
   ;;     [:operator \"*\"]
   ;;     [:open-paren \"(\"] [:number \"3\"] [:operator \"-\"]
   ;;     [:number \"4\"] [:close-paren \")\"]]
   ```"
  [input]
  (let [tokens (->> input
                   (re-seq #"-?\d+|[()+\-*/]")
                   (remove empty?)
                   vec)]
    (mapv (fn [token]
            (cond
              (re-matches #"-?\d+" token) [:number token]
              (contains? #{"+" "-" "*" "/"} token) [:operator token]
              (= "(" token) [:open-paren token]
              (= ")" token) [:close-paren token]
              :else [:unknown token]))
          tokens))) 