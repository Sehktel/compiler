(ns compiler.ast
  (:require 
    [clojure.string :as str]
    [compiler.ast :as ast]
    [compiler.parser :as parser]))

(defn- create-branch
  "Создаёт символы для отрисовки ветвей дерева"
  [is-last?]
  (if is-last?
    "└── "
    "├── "))

(defn- create-indent
  "Создаёт отступы для уровней вложенности"
  [parents-last?]
  (str/join (map #(if % "    " "│   ") (butlast parents-last?))))

(defn- node-to-string
  "Преобразует узел AST в строковое представление"
  [node]
  (cond
    (map? node) (str (:type node))
    (vector? node) "Vector"
    :else (str node)))

(defn print-ast
  "Красиво печатает AST в виде дерева с расширенной информацией"
  ([ast] (print-ast ast [] true))
  ([node parents-last? is-last?]
   (let [indent (create-indent parents-last?)
         branch (create-branch is-last?)
         node-str (cond 
                    (map? node) 
                    (str (:type node) 
                         (when-let [val (:value node)] 
                           (str " (value: " val ")")))
                    (vector? node) "Vector"
                    :else (str node))]
     
     ; Печатаем текущий узел
     (println (str indent branch node-str))
     
     ; Рекурсивно обрабатываем дочерние узлы
     (when (map? node)
       (let [children (dissoc node :type :value)
             last-idx (dec (count children))]
         (doseq [[idx [k v]] (map-indexed vector children)]
           (print (str indent (if is-last? "    " "│   ") (str k ": ")))
           (print-ast v 
                     (conj parents-last? is-last?)
                     (= idx last-idx))))))))

;; Добавляем функцию для парсинга и печати AST
(defn parse-and-print-ast 
  "Парсит входной код и печатает его AST"
  [input]
  (let [ast (parser/parse input)]
    (when ast
      (println "Abstract Syntax Tree:")
      (print-ast ast)
      ast)))

