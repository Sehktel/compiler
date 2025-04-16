(ns user
  (:require 
   [compiler.parser :as parser]
   [compiler.ast :as ast]
   [clojure.java.io :as io]))

(defn read-c-file 
  "Читает содержимое C-файла"
  [filename]
  (slurp (io/resource (str "c_sources/" filename))))

(defn test-ast 
  "Тестирует парсинг и визуализацию AST для указанного файла"
  [filename]
  (let [c-code (read-c-file filename)]
    (println (str "Parsing file: " filename))
    (println "-------------------")
    (ast/parse-and-print-ast c-code)))

;; Функции для интерактивного тестирования AST
(defn parse-file 
  "Парсит C-файл и возвращает AST"
  [filename]
  (let [c-code (read-c-file filename)]
    (parser/parse c-code)))

;; Примеры использования
(defn run-ast-tests []
  (println "Тестирование парсинга простой арифметики:")
  (test-ast "simple_arithmetic.c")
  
  (println "\nТестирование парсинга управляющих структур:")
  (test-ast "control_flow.c"))

;; Автоматический запуск тестов при загрузке namespace
(run-ast-tests) 