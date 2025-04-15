(ns compiler.ast
  (:require [clojure.string :as str])
  (:require '[compiler.ast :as ast] :reload)
  (:require '[compiler.parser :as parser] :reload))

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
  "Красиво печатает AST в виде дерева"
  ([ast] (print-ast ast [] true))
  ([node parents-last? is-last?]
   (let [indent (create-indent parents-last?)
         branch (create-branch is-last?)
         node-str (node-to-string node)]
     
     ; Печатаем текущий узел
     (println (str indent branch node-str))
     
     ; Рекурсивно обрабатываем дочерние узлы
     (when (map? node)
       (let [children (dissoc node :type)
             last-idx (dec (count children))]
         (doseq [[idx [k v]] (map-indexed vector children)]
           (print (str indent (if is-last? "    " "│   ") (str k ": ")))
           (print-ast v 
                     (conj parents-last? is-last?)
                     (= idx last-idx))))))))

