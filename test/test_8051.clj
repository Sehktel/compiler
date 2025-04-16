(ns test-8051
  (:require [compiler.parser :refer [parse]]
            [compiler.ast :refer [print-ast]]
            [compiler.lexer :refer [tokenize]]
            [compiler.pre_processor :refer [preprocess]]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing run-tests]]))

(defn test-8051-file
  "Тестирует обработку файла с кодом для 8051"
  [filename]
  (if (.exists (io/file filename))
    (do
      (println "\nТестирование файла:" filename)
      (println "Содержимое:")
      (let [content (slurp filename :encoding "UTF-8")
            _ (println content)
            
            ;; Препроцессинг
            preprocessed (preprocess content)
            _ (println "\nПосле препроцессора:")
            _ (println preprocessed)
            
            ;; Токенизация
            tokens (tokenize preprocessed)
            _ (println "\nТокены:")
            _ (doseq [token tokens]
                (println (format "%-12s %s" (name (first token)) (second token))))
            
            ;; Построение AST
            _ (println "\nПостроение AST:")
            ast (parse preprocessed)]
        
        (if ast
          (do
            (println "\nAST успешно построено:")
            (print-ast ast)
            ast)
          (println "Ошибка при построении AST"))))
    (println "Файл не найден:" filename)))

(defn -main
  "Запускает тестирование файла с кодом для 8051"
  [& args]
  (if (first args)
    (test-8051-file (first args))
    (test-8051-file "test/c4ast/8051_sample.c"))) 