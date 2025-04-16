(ns test_8051
  (:require [compiler.lexer :refer [tokenize]]
            [compiler.pre_processor :refer [preprocess]]
            [compiler.parser :refer [parse]]
            [compiler.ast :refer [print-ast]]))

(defn test-8051-code
  "Тестирует лексер и парсер с кодом для 8051"
  [file-path]
  (println "Тестирование файла:" file-path)
  (let [code (slurp file-path)
        _ (println "\nИсходный код:")
        _ (println code)
        
        ;; Препроцессор
        processed (preprocess code)
        _ (println "\nПосле препроцессора:")
        _ (println processed)
        
        ;; Токенизация
        tokens (tokenize processed)
        _ (println "\nТокены:")
        _ (doseq [token tokens]
            (println token))
        
        ;; Парсинг
        ast (parse processed)
        _ (println "\nAST:")
        _ (if ast
            (print-ast ast)
            (println "Ошибка при построении AST"))]
    
    ;; Возвращаем токены и AST для дальнейшего анализа
    {:tokens tokens
     :ast ast}))

(defn -main
  "Точка входа для тестирования 8051 кода"
  [& args]
  (let [file-path (or (first args) "test/test_file.c")]
    (test-8051-code file-path))) 