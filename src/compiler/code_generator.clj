(ns compiler.code-generator
  "Генератор ассемблерного кода для микроконтроллера 8051.
   
   ## Основные возможности
   - Трансляция AST в ассемблерный код
   - Поддержка специфических регистров 8051
   - Генерация инструкций с учетом архитектуры микроконтроллера"
  (:require [compiler.ast :as ast]
            [clojure.string :as str]))

;; Регистры общего назначения 8051
(def ^:private general-registers 
  {:a "A"   ; Аккумулятор
   :b "B"   ; Регистр B
   :r0 "R0" ; Регистр R0
   :r1 "R1" ; Регистр R1
   :r2 "R2" ; Регистр R2
   :r3 "R3" ; Регистр R3
   :r4 "R4" ; Регистр R4
   :r5 "R5" ; Регистр R5
   :r6 "R6" ; Регистр R6
   :r7 "R7" ; Регистр R7
   })

;; Специальные функциональные регистры (SFR)
(def ^:private sfr-registers
  {:sp "SP"    ; Указатель стека
   :dpl "DPL"  ; Младший байт указателя данных
   :dph "DPH"  ; Старший байт указателя данных
   :psw "PSW"  ; Регистр состояния программы
   })

;; Таблица трансляции операторов
(def ^:private op-translation
  {:plus "ADD"
   :minus "SUBB"
   :asterisk "MUL"
   :slash "DIV"
   :bit_and "ANL"
   :bit_or "ORL"
   :bit_xor "XRL"})

(defn- translate-binary-op
  "Транслирует бинарную операцию в ассемблерную инструкцию"
  [op]
  (get op-translation op 
       (throw (ex-info "Неподдерживаемая операция" {:op op}))))

(defn generate-instruction
  "Генерирует базовую ассемблерную инструкцию"
  [instruction & args]
  (str instruction " " (str/join ", " args)))

(defmulti generate-code 
  "Мультиметод генерации кода для различных типов узлов AST"
  (fn [node] 
    (cond 
      (instance? ast/BinaryOp node) :binary-op
      (instance? ast/Num node) :number
      (instance? ast/Variable node) :variable
      :else :default)))

(defmethod generate-code :binary-op
  [node]
  (let [op (translate-binary-op (:op node))
        left (generate-code (:left node))
        right (generate-code (:right node))]
    (generate-instruction op left right)))

(defmethod generate-code :number
  [node]
  (str "#" (:value node)))

(defmethod generate-code :variable
  [node]
  (get general-registers (keyword (:name node)) 
       (get sfr-registers (keyword (:name node)) 
            (:name node))))

(defmethod generate-code :default
  [node]
  (throw (ex-info "Неподдерживаемый тип узла" {:node node})))

(defn generate-assembly
  "Главная функция генерации ассемблерного кода"
  [ast]
  (let [code-lines (atom [])]
    (when (= (:type ast) :program)
      (doseq [statement (:body ast)]
        (let [asm-code (generate-code statement)]
          (swap! code-lines conj asm-code))))
    (str/join "\n" @code-lines)))

(defn compile-to-8051
  "Полный цикл компиляции в ассемблер 8051"
  [ast]
  (generate-assembly ast)) 