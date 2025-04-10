(ns compiler.lexer
  (:require [clojure.string :as str]))

;; Определение типов токенов
(def token-types
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

;; Функция для проверки, является ли строка ключевым словом
(defn is-keyword? [s]
  (some #(= s %) (:keyword token-types)))

;; Функция для проверки, является ли строка оператором
(defn operator? [s]
  (some #(= s %) (:operator token-types)))

;; Функция для проверки, является ли строка разделителем
(defn separator? [s]
  (some #(= s %) (:separator token-types)))

;; Функция для определения типа токена
(defn get-token-type [token]
  (cond
    (is-keyword? token) :keyword
    (operator? token) :operator
    (separator? token) :separator
    (re-matches (:identifier token-types) token) :identifier
    (re-matches (:number token-types) token) :number
    (re-matches (:string token-types) token) :string
    (re-matches (:comment token-types) token) :comment
    :else :unknown))

;; Основная функция лексера
(defn lex [input]
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

;; Функция для тестирования лексера
(defn test-lexer []
  (let [test-code "void timer0_isr() interrupt 2 {
    // Простой обработчик прерывания
    P1 ^= 0x01;  // Инвертируем бит
}"
        tokens (lex test-code)]
    (doseq [token tokens]
      (println (format "Тип: %-10s Значение: %s" 
                      (name (:type token)) 
                      (:value token))))))

;; Функция для токенизации входной строки
(defn tokenize [input]
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