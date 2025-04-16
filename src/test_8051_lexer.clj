(ns test-8051-lexer
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:gen-class))

(def token-types
  {:sfr_keyword ["sfr"]
   :sbit_keyword ["sbit"]
   :void_keyword ["void"]
   :while_keyword ["while"]
   :main_keyword ["main"]
   :interrupt_keyword ["interrupt"]
   :identifier #"[a-zA-Z_][a-zA-Z0-9_]*"
   :number #"0[xX][0-9a-fA-F]+|\d+"
   :equal ["="]
   :semicolon [";"]
   :open_round_bracket ["("]
   :close_round_bracket [")"]
   :open_curly_bracket ["{"]
   :close_curly_bracket ["}"]
   :comment #"//[^\n]*|/\*[\s\S]*?\*/"})

(defn is-keyword? [s]
  (some #(some (partial = s) %) 
        (vals (select-keys token-types 
                          [:sfr_keyword
                           :sbit_keyword
                           :void_keyword
                           :while_keyword
                           :main_keyword
                           :interrupt_keyword]))))

(defn separator? [s]
  (some #(some (partial = s) %) 
        (vals (select-keys token-types 
                          [:open_round_bracket 
                           :close_round_bracket 
                           :open_curly_bracket
                           :close_curly_bracket 
                           :semicolon]))))

(defn operator? [s]
  (some #(some (partial = s) %) 
        (vals (select-keys token-types 
                          [:equal]))))

(defn get-token-type [token]
  (cond
    (is-keyword? token) :keyword
    (operator? token) :operator
    (separator? token) :separator
    (re-matches (:identifier token-types) token) :identifier
    (re-matches (:number token-types) token) :number
    :else :unknown))

(defn find-exact-token-type [value]
  (if (str/blank? value)
    :unknown
    (or
     (first 
      (filter 
       #(and (not (#{:identifier :number} %))
             (let [token-val (get token-types %)]
               (if (vector? token-val)
                 (some (partial = value) token-val)
                 false)))
       (keys token-types)))
     
     (cond
       (= value "while") :while_keyword
       (= value "void") :void_keyword
       (= value "main") :main_keyword
       (re-matches (:identifier token-types) value) :identifier
       (re-matches (:number token-types) value) :number
       :else :unknown)))

(defn pre-process-line [line]
  ;; Убираем комментарии
  (let [line (str/replace line #"//.*$" "")
        line (str/replace line #"/\*.*?\*/" "")]
    ;; Добавляем пробелы вокруг операторов и разделителей
    (-> line 
      (str/replace #";" " ; ")
      (str/replace #"=" " = ")
      (str/replace #"\(" " ( ")
      (str/replace #"\)" " ) ")
      (str/replace #"\{" " { ")
      (str/replace #"\}" " } "))))

(defn tokenize-line [line]
  (let [tokens (atom [])
        pos (atom 0)]
    (while (< @pos (count line))
      (let [current-char (get line @pos)]
        (cond
          ;; Пропускаем пробелы
          (Character/isWhitespace current-char)
          (swap! pos inc)
          
          ;; Обработка комментариев
          (and (= current-char \/) (< (inc @pos) (count line)) (= (get line (inc @pos)) \/))
          (swap! pos + (count line)) ;; Пропускаем до конца строки
          
          (and (= current-char \/) (< (inc @pos) (count line)) (= (get line (inc @pos)) \*))
          (let [end-pos (str/index-of line "*/" (+ @pos 2))]
            (if end-pos
              (swap! pos + (+ end-pos 2))
              (swap! pos + (count line)))) ;; Пропускаем до конца многострочного комментария
          
          ;; Обработка идентификаторов
          (or (Character/isLetter current-char) (= current-char \_))
          (let [start-pos @pos
                end-pos (loop [i (inc @pos)]
                          (if (and (< i (count line))
                                  (or (Character/isLetterOrDigit (get line i))
                                     (= (get line i) \_)))
                            (recur (inc i))
                            i))
                identifier (subs line start-pos end-pos)
                token-type (find-exact-token-type identifier)]
            (swap! tokens conj [token-type identifier])
            (reset! pos end-pos))
          
          ;; Обработка чисел
          (Character/isDigit current-char)
          (let [start-pos @pos
                end-pos (loop [i (inc @pos)]
                          (if (and (< i (count line))
                                  (or (Character/isDigit (get line i))
                                     (and (or (= (get line start-pos) \0)
                                             (and (>= i (+ start-pos 2))
                                                 (= (get line start-pos) \0)
                                                 (or (= (get line (inc start-pos)) \x)
                                                     (= (get line (inc start-pos)) \X)))
                                         (contains? #{\a \b \c \d \e \f \A \B \C \D \E \F \0 \1 \2 \3 \4 \5 \6 \7 \8 \9}
                                                  (get line i)))))
                            (recur (inc i))
                            i))
                number (subs line start-pos end-pos)]
            (swap! tokens conj [:number number])
            (reset! pos end-pos))
          
          ;; Обработка одиночных символов-разделителей
          (contains? #{\( \) \{ \} \;} current-char)
          (let [token-type (find-exact-token-type (str current-char))]
            (swap! tokens conj [token-type (str current-char)])
            (swap! pos inc))
          
          ;; Обработка операторов
          (= current-char \=)
          (do
            (swap! tokens conj [:equal "="])
            (swap! pos inc))
          
          ;; Если ничего не подошло, пропускаем символ
          :else
          (swap! pos inc))))
    @tokens))

(defn tokenize [code]
  (if (nil? code)
    []
    (let [lines (str/split-lines code)
          all-tokens (atom [])]
      (doseq [line lines]
        (let [tokens (tokenize-line line)]
          (when (seq tokens)
            (swap! all-tokens into tokens))))
      @all-tokens)))

(defn test-lexer [file-path]
  (println "Testing file:" file-path)
  (let [code (slurp file-path)]
    (println "\nSource code:")
    (println code)
    
    (println "\nTokens:")
    (let [tokens (tokenize code)]
      (doseq [token tokens]
        (println token))
      tokens)))

(defn -main [& args]
  (let [file-path (or (first args) "test/test_file.c")]
    (test-lexer file-path))) 