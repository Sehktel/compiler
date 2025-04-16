(ns compiler.ast
  (:require 
    [clojure.string :as str]))

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

;; =============================================
;; Типы узлов AST для стандартного C
;; =============================================

(defrecord Num [value])
(defrecord BinaryOp [op left right])
(defrecord Parens [expr])
(defrecord Variable [name])
(defrecord FunctionCall [name args])
(defrecord If [condition then else])
(defrecord While [condition body])
(defrecord For [init condition update body])
(defrecord Return [expr])
(defrecord UnaryOp [op expr])
(defrecord Block [statements])

;; =============================================
;; Типы узлов AST для микроконтроллера 8051
;; =============================================

;; Объявление регистра специальных функций (Special Function Register)
;; sfr SP = 0x81;
(defrecord SfrDeclaration [name value])

;; Объявление бита специальных функций (Special Function Register Bit)
;; sbit P1_0 = 0x90;
(defrecord SbitDeclaration [name value])

;; Объявление функции с атрибутом прерывания
;; void P14 (void) interrupt IE0_VECTOR { ... }
(defrecord InterruptFunction [return-type name params vector-id body])

;; =============================================
;; Функции для печати AST
;; =============================================

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
                    
                    ;; Случаи записей для AST узлов
                    (instance? Num node)
                    (str "Num (value: " (:value node) ")")
                    
                    (instance? BinaryOp node)
                    (str "BinaryOp (op: " (:op node) ")")
                    
                    (instance? Parens node)
                    "Parens"
                    
                    (instance? Variable node)
                    (str "Variable (name: " (:name node) ")")
                    
                    (instance? FunctionCall node)
                    (str "FunctionCall (name: " (:name node) ")")
                    
                    (instance? If node)
                    "If"
                    
                    (instance? While node)
                    "While"
                    
                    (instance? For node)
                    "For"
                    
                    (instance? Return node)
                    "Return"
                    
                    (instance? UnaryOp node)
                    (str "UnaryOp (op: " (:op node) ")")
                    
                    (instance? Block node)
                    (str "Block (statements: " (count (:statements node)) ")")
                    
                    ;; Случаи записей для 8051-специфичных типов
                    (instance? SfrDeclaration node)
                    (str "SfrDeclaration (name: " (:name node) ", value: " (:value node) ")")
                    
                    (instance? SbitDeclaration node)
                    (str "SbitDeclaration (name: " (:name node) ", value: " (:value node) ")")
                    
                    (instance? InterruptFunction node)
                    (str "InterruptFunction (name: " (:name node) 
                         ", vector: " (:vector-id node) ")")
                    
                    (vector? node) 
                    "Vector"
                    
                    :else 
                    (str node))]
     
     ; Печатаем текущий узел
     (println (str indent branch node-str))
     
     ; Рекурсивно обрабатываем дочерние узлы
     (cond
       (map? node)
       (let [children (dissoc node :type :value)
             last-idx (dec (count children))]
         (doseq [[idx [k v]] (map-indexed vector children)]
           (print (str indent (if is-last? "    " "│   ") (str k ": ")))
           (print-ast v 
                     (conj parents-last? is-last?)
                     (= idx last-idx))))
       
       (instance? BinaryOp node)
       (do
         (print (str indent (if is-last? "    " "│   ") "left: "))
         (print-ast (:left node) (conj parents-last? is-last?) false)
         (print (str indent (if is-last? "    " "│   ") "right: "))
         (print-ast (:right node) (conj parents-last? is-last?) true))
       
       (instance? Parens node)
       (do
         (print (str indent (if is-last? "    " "│   ") "expr: "))
         (print-ast (:expr node) (conj parents-last? is-last?) true))
       
       (instance? If node)
       (do
         (print (str indent (if is-last? "    " "│   ") "condition: "))
         (print-ast (:condition node) (conj parents-last? is-last?) false)
         (print (str indent (if is-last? "    " "│   ") "then: "))
         (print-ast (:then node) (conj parents-last? is-last?) (nil? (:else node)))
         (when (:else node)
           (print (str indent (if is-last? "    " "│   ") "else: "))
           (print-ast (:else node) (conj parents-last? is-last?) true)))
       
       (instance? While node)
       (do
         (print (str indent (if is-last? "    " "│   ") "condition: "))
         (print-ast (:condition node) (conj parents-last? is-last?) false)
         (print (str indent (if is-last? "    " "│   ") "body: "))
         (print-ast (:body node) (conj parents-last? is-last?) true))
       
       (instance? For node)
       (do
         (print (str indent (if is-last? "    " "│   ") "init: "))
         (print-ast (:init node) (conj parents-last? is-last?) false)
         (print (str indent (if is-last? "    " "│   ") "condition: "))
         (print-ast (:condition node) (conj parents-last? is-last?) false)
         (print (str indent (if is-last? "    " "│   ") "update: "))
         (print-ast (:update node) (conj parents-last? is-last?) false)
         (print (str indent (if is-last? "    " "│   ") "body: "))
         (print-ast (:body node) (conj parents-last? is-last?) true))
       
       (instance? Return node)
       (do
         (print (str indent (if is-last? "    " "│   ") "expr: "))
         (print-ast (:expr node) (conj parents-last? is-last?) true))
       
       (instance? UnaryOp node)
       (do
         (print (str indent (if is-last? "    " "│   ") "expr: "))
         (print-ast (:expr node) (conj parents-last? is-last?) true))
       
       (instance? Block node)
       (let [statements (:statements node)
             last-idx (dec (count statements))]
         (doseq [[idx stmt] (map-indexed vector statements)]
           (print (str indent (if is-last? "    " "│   ") 
                       "statement " (inc idx) ": "))
           (print-ast stmt 
                      (conj parents-last? is-last?) 
                      (= idx last-idx))))
       
       (instance? FunctionCall node)
       (let [args (:args node)
             last-idx (dec (count args))]
         (doseq [[idx arg] (map-indexed vector args)]
           (print (str indent (if is-last? "    " "│   ") 
                       "arg " (inc idx) ": "))
           (print-ast arg 
                      (conj parents-last? is-last?) 
                      (= idx last-idx))))
       
       ;; Обработка 8051-специфичных типов
       (instance? InterruptFunction node)
       (do
         (print (str indent (if is-last? "    " "│   ") "return-type: "))
         (print-ast (:return-type node) (conj parents-last? is-last?) false)
         (print (str indent (if is-last? "    " "│   ") "vector-id: "))
         (print-ast (:vector-id node) (conj parents-last? is-last?) false)
         (print (str indent (if is-last? "    " "│   ") "body: "))
         (print-ast (:body node) (conj parents-last? is-last?) true))
       
       (vector? node)
       (let [last-idx (dec (count node))]
         (doseq [[idx item] (map-indexed vector node)]
           (print (str indent (if is-last? "    " "│   ") 
                       "item " (inc idx) ": "))
           (print-ast item 
                      (conj parents-last? is-last?) 
                      (= idx last-idx))))))))

;; Добавляем функцию для парсинга и печати AST
(defn parse-and-print-ast 
  "Парсит входной код и печатает его AST"
  [input parser-fn]
  (let [ast (parser-fn input)]
    (when ast
      (println "Abstract Syntax Tree:")
      (print-ast ast)
      ast)))

