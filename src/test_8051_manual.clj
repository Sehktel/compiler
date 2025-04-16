(ns test_8051_manual
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:gen-class))

;; Наш упрощенный токенизатор специально для 8051
(defn tokenize-8051 [code]
  (let [lines (str/split-lines code)
        result (atom [])]
    (doseq [line lines]
      ;; Убираем комментарии
      (let [clean-line (-> line
                         (str/replace #"//.*$" "")
                         (str/replace #"/\*.*?\*/" ""))]
        ;; Добавляем пробелы вокруг специальных символов
        (let [spaced-line (-> clean-line
                           (str/replace #";" " ; ")
                           (str/replace #"=" " = ")
                           (str/replace #"\(" " ( ")
                           (str/replace #"\)" " ) ")
                           (str/replace #"\{" " { ")
                           (str/replace #"\}" " } "))
              tokens (filter #(not (str/blank? %)) 
                             (str/split spaced-line #"\s+"))]
          (doseq [token tokens]
            (let [token-type (cond 
                              (= token "sfr") :sfr_keyword
                              (= token "sbit") :sbit_keyword
                              (= token "void") :void_keyword
                              (= token "while") :while_keyword
                              (= token "main") :main_keyword
                              (= token "interrupt") :interrupt_keyword
                              (= token "(") :open_paren
                              (= token ")") :close_paren
                              (= token "{") :open_brace
                              (= token "}") :close_brace
                              (= token ";") :semicolon
                              (= token "=") :equal
                              (re-matches #"\d+" token) :number
                              (re-matches #"0[xX][0-9a-fA-F]+" token) :hex_number
                              (re-matches #"[a-zA-Z_][a-zA-Z0-9_]*" token) :identifier
                              :else :unknown)]
              (swap! result conj [token-type token]))))))
    @result))

(defn test-file [file-path]
  (println "Тестирование файла:" file-path)
  (let [code (slurp file-path)]
    (println "\nИсходный код:")
    (println code)
    
    (println "\nТокены:")
    (let [tokens (tokenize-8051 code)]
      (doseq [token tokens]
        (println token))
      
      (println "\nОбнаружено:")
      (println "- Ключевые слова sfr:" (count (filter #(= (first %) :sfr_keyword) tokens)))
      (println "- Ключевые слова sbit:" (count (filter #(= (first %) :sbit_keyword) tokens)))
      (println "- Идентификаторы:" (count (filter #(= (first %) :identifier) tokens)))
      (println "- Числа:" (count (filter #(or (= (first %) :number) (= (first %) :hex_number)) tokens)))
      tokens)))

(defn -main [& args]
  (let [file-path (or (first args) "test/test_file.c")]
    (test-file file-path))) 