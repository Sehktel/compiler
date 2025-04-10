(ns compiler.ast
  (:gen-class))

;; Предварительное объявление всех функций
(declare print-ast)

;; Определение типов узлов AST (Abstract Syntax Tree)
;; Num - узел для числовых литералов
(defrecord Num [value])

;; BinaryOp - узел для бинарных операций (+, -, *, /)
(defrecord BinaryOp [op left right])

;; LogicalOp - узел для логических операций (&&, ||, !)
(defrecord LogicalOp [op left right])

;; While - узел для цикла while
(defrecord While [condition body])

;; Parens - узел для выражений в скобках
(defrecord Parens [expr])

;; Variable - узел для переменных
(defrecord Variable [name])

;; UnaryOp - узел для унарных операций (-, ! и т.д.)
(defrecord UnaryOp [op expr])

;; FunctionCall - узел для вызова функций
(defrecord FunctionCall [name args])

;; If -- узел для условного оператора
(defrecord If [condition then else])

;; Return - узел для оператора return
(defrecord Return [value])

;; Block - узел для блока кода
(defrecord Block [statements])

;; Function - узел для определения функции
(defrecord Function [name params body])

;; For - узел для цикла for
(defrecord For [init condition update body])

;; ;; interrupt - узел для оператора interrupt
;; (defrecord Interrupt [])

(defn print-ast [ast & [indent]]
  (let [indent (or indent 0)
        spaces (apply str (repeat indent "  "))]
    (cond
      (instance? Num ast)
      (println spaces "Num:" (:value ast))
      
      (instance? BinaryOp ast)
      (do
        (println spaces "BinaryOp:" (:op ast))
        (print-ast (:left ast) (+ indent 1))
        (print-ast (:right ast) (+ indent 1)))
      
      (instance? Parens ast)
      (do
        (println spaces "Parens:")
        (print-ast (:expr ast) (+ indent 1)))
      
      (instance? LogicalOp ast)
      (do
        (println spaces "LogicalOp:" (:op ast))
        (print-ast (:left ast) (+ indent 1))
        (print-ast (:right ast) (+ indent 1)))
      
      (instance? While ast)
      (do
        (println spaces "While:")
        (print-ast (:condition ast) (+ indent 1))
        (print-ast (:body ast) (+ indent 1)))
      
      (instance? If ast)
      (do
        (println spaces "If:")
        (print-ast (:condition ast) (+ indent 1))
        (print-ast (:then ast) (+ indent 1))
        (when (:else ast)
          (print-ast (:else ast) (+ indent 1))))
      
      (instance? Return ast)
      (do
        (println spaces "Return:")
        (print-ast (:value ast) (+ indent 1)))
      
      (instance? Block ast)
      (do
        (println spaces "Block:")
        (doseq [stmt (:statements ast)]
          (print-ast stmt (+ indent 1))))
      
      (instance? Function ast)
      (do
        (println spaces "Function:" (:name ast))
        (println spaces "  Params:" (:params ast))
        (print-ast (:body ast) (+ indent 1)))
      
      (instance? For ast)
      (do
        (println spaces "For:")
        (print-ast (:init ast) (+ indent 1))
        (print-ast (:condition ast) (+ indent 1))
        (print-ast (:update ast) (+ indent 1))
        (print-ast (:body ast) (+ indent 1)))
      
      :else
      (println spaces ast))))

(println "\nПримеры AST:")
(print-ast (->Num 1))
(print-ast (->BinaryOp "+" (->Num 1) (->Num 2)))
(print-ast (->Parens (->BinaryOp "*" (->Num 3) (->Num 4))))
(print-ast (->BinaryOp "+" 
                      (->Num 1)
                      (->BinaryOp "*" (->Num 2) (->Num 3))))

