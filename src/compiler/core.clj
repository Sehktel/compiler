(ns compiler.core
  (:require [compiler.parser :refer [parse]]
            [compiler.ast :refer [print-ast]]
            [compiler.lexer :refer [tokenize]]
            [clojure.java.io :as io])
  (:gen-class))

(defn read-test-files
  "Читает все тестовые файлы из директории ./test/c4ast/"
  []
  (let [dir (io/file "test/c4ast")
        files (filter #(.endsWith (.getName %) ".c") 
                     (file-seq dir))]
    (sort-by #(.getName %) files)))

(defn tokenize-file
  "Токенизирует файл и выводит список токенов без построения AST"
  [filename]
  (if (.exists (io/file filename))
    (do
      (println "\nТокенизация файла:" filename)
      (println "Содержимое:")
      (let [content (slurp filename)
            tokens (tokenize content)]
        (println content)
        (println "\nТокены:")
        (doseq [token tokens]
          (println (format "%-12s %s" (name (first token)) (second token))))))
    (println "Файл не найден:" filename)))

(defn test-ast-construction
  "Тестирует построение AST для каждого тестового файла"
  []
  (println "\n=== Тестирование построения AST ===")
  (doseq [file (read-test-files)]
    (println "\nФайл:" (.getName file))
    (println "Содержимое:")
    (println (slurp file))
    (println "\nAST:")
    (let [content (slurp file)
          tokens (tokenize content)]
      (println "Токены:" tokens)
      (try
        (let [ast (parse content)]
          (if ast
            (print-ast ast)
            (println "Ошибка при построении AST: результат парсинга null")))
        (catch Exception e
          (println "Ошибка при построении AST: " (.getMessage e))
          (println "Тип исключения: " (class e))
          (println "Стек вызовов:")
          (.printStackTrace e))))))

(defn analyze-file
  "Анализирует указанный файл и выводит его AST"
  [filename]
  (if (.exists (io/file filename))
    (do
      (println "\nАнализ файла:" filename)
      (println "Содержимое:")
      (let [content (slurp filename)
            tokens (tokenize content)]
        (println content)
        (println "\nТокены:")
        (println tokens)
        (println "\nAST:")
        (try
          (let [ast (parse content)]
            (if ast
              (print-ast ast)
              (println "Ошибка при построении AST: результат парсинга null")))
          (catch Exception e
            (println "Ошибка при построении AST: " (.getMessage e))
            (println "Тип исключения: " (class e))
            (println "Стек вызовов:")
            (.printStackTrace e)))))
    (println "Файл не найден:" filename)))

(defn -main
  "Основная функция приложения. Запускает тесты AST или анализирует указанный файл."
  [& args]
  (if (empty? args)
    (do
      (println "Запуск тестирования AST деревьев...")
      (test-ast-construction))
    (let [cmd (first args)]
      (cond
        (= cmd "tokenize") (if (second args)
                              (tokenize-file (second args))
                              (println "Укажите файл для токенизации"))
        :else (analyze-file cmd)))))
