(ns compiler.core
  (:require [compiler.parser :refer [parse]]
            [compiler.ast :refer [print-ast]]
            [compiler.lexer :refer [tokenize]]
            [clojure.java.io :as io])
  (:gen-class))

;; Установка кодировки UTF-8 для вывода
(alter-var-root #'*out* (constantly (-> System/out
                                      (java.io.OutputStreamWriter. "UTF-8")
                                      java.io.BufferedWriter.
                                      java.io.PrintWriter.)))

(def debug-levels #{:info :warn :error})

(def debug-flags (atom {:verbose false
                       :trace false}))

(defn parse-args
  "Разбор аргументов командной строки
   Поддерживает флаги:
   --debug       : включает подробный вывод
   --trace      : включает трассировку"
  [args]
  (loop [remaining-args args]
    (when (seq remaining-args)
      (case (first remaining-args)
        "--debug" (do (swap! debug-flags assoc :verbose true)
                     (recur (rest remaining-args)))
        "--trace" (do (swap! debug-flags assoc :trace true)
                     (recur (rest remaining-args)))
        ;; Продолжаем обработку других аргументов
        (recur (rest remaining-args))))))

(defn debug-print
  "Вывод отладочной информации"
  [msg]
  (when (:verbose @debug-flags)
    (println "[DEBUG]:" msg)))

(defn trace-print
  "Вывод информации трассировки"
  [msg]
  (when (:trace @debug-flags)
    (println "[TRACE]:" msg)))

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
      (let [content (slurp filename :encoding "UTF-8")
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
    (println (slurp file :encoding "UTF-8"))
    (println "\nAST:")
    (let [content (slurp file :encoding "UTF-8")
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
      (let [content (slurp filename :encoding "UTF-8")
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

(defn structured-debug [level category msg]
  (when (:verbose @debug-flags)
    (println (format "[%s][%s]: %s" 
                    (name level) 
                    (name category) 
                    msg))))

(defn -main
  "Точка входа в программу"
  [& args]
  ;; Сначала обрабатываем флаги отладки
  (parse-args args)
  (debug-print "Запуск компилятора в режиме отладки")
  (trace-print "Начало трассировки выполнения")
  
  ;; Фильтруем аргументы, убирая флаги отладки
  (let [real-args (remove #(or (= % "--debug") 
                              (= % "--trace")) 
                         args)]
    (if (empty? real-args)
      (do
        (println "Запуск тестирования AST деревьев...")
        (test-ast-construction))
      (let [cmd (first real-args)]
        (cond
          (= cmd "tokenize") (if (second real-args)
                              (tokenize-file (second real-args))
                              (println "Укажите файл для токенизации"))
          :else (analyze-file cmd))))))
