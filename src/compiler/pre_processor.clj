(ns compiler.pre_processor
  (:require [clojure.string :as str]))

;; Определение состояний препроцессора
(def preprocessor-states
  {:normal 0
   :define 1
   :ifdef 2
   :ifndef 3
   :include 4})

;; Хранилище макросов
(def macros (atom {}))

;; Хранилище условной компиляции
(def conditional-stack (atom []))

;; Функция для обработки директивы #define
(defn process-define [line]
  (let [[_ macro-name & macro-value] (str/split line #"\s+")]
    (when macro-name
      (swap! macros assoc macro-name (str/join " " macro-value))
      "")))

;; Функция для обработки директивы #undef
(defn process-undef [line]
  (let [[_ macro-name] (str/split line #"\s+")]
    (when macro-name
      (swap! macros dissoc macro-name)
      "")))

;; Функция для обработки директивы #ifdef
(defn process-ifdef [line]
  (let [[_ macro-name] (str/split line #"\s+")]
    (swap! conditional-stack conj (contains? @macros macro-name))
    (if (contains? @macros macro-name) "" nil)))

;; Функция для обработки директивы #ifndef
(defn process-ifndef [line]
  (let [[_ macro-name] (str/split line #"\s+")]
    (swap! conditional-stack conj (not (contains? @macros macro-name)))
    (if (not (contains? @macros macro-name)) "" nil)))

;; Функция для обработки директивы #endif
(defn process-endif [_]
  (swap! conditional-stack pop)
  "")

;; Функция для обработки директивы #include
(defn process-include [line]
  (let [[_ filename] (re-find #"#include\s*[<\"]([^>\"]+)[>\"]" line)]
    (when filename
      (try
        ;; Проверяем, есть ли файл в текущей директории
        (let [file (java.io.File. filename)]
          (if (.exists file)
            (slurp file)
            ;; Если файл не найден, проверяем предопределенные включаемые файлы для 8051
            (case filename
              "reg2051.h" (str "#ifndef __REG2051_H__\n"
                              "#define __REG2051_H__\n\n"
                              "/*------------------------------------------------\n"
                              "Vectors\n"
                              "------------------------------------------------*/\n"
                              "#define IE0_VECTOR	0\n"
                              "#define TF0_VECTOR	1\n"
                              "#define IE1_VECTOR	2\n"
                              "#define TF1_VECTOR	3\n"
                              "#define SIO_VECTOR	4\n\n"
                              "/*------------------------------------------------\n"
                              "Byte Registers\n"
                              "------------------------------------------------*/\n"
                              "sfr P0   = 0x80;\n"
                              "sfr SP   = 0x81;\n"
                              "sfr DPL  = 0x82;\n"
                              "sfr DPH  = 0x83;\n"
                              "sfr PCON = 0x87;\n"
                              "sfr TCON = 0x88;\n"
                              "sfr TMOD = 0x89;\n"
                              "sfr TL0  = 0x8A;\n"
                              "sfr TL1  = 0x8B;\n"
                              "sfr TH0  = 0x8C;\n"
                              "sfr TH1  = 0x8D;\n"
                              "sfr P1   = 0x90;\n"
                              "sfr SCON = 0x98;\n"
                              "sfr SBUF = 0x99;\n"
                              "sfr P2   = 0xA0;\n"
                              "sfr IE   = 0xA8;\n"
                              "sfr P3   = 0xB0;\n"
                              "sfr IP   = 0xB8;\n"
                              "sfr PSW  = 0xD0;\n"
                              "sfr ACC  = 0xE0;\n"
                              "sfr B    = 0xF0;\n\n"
                              "/*------------------------------------------------\n"
                              "P1 Bit Registers\n"
                              "------------------------------------------------*/\n"
                              "sbit P1_0 = 0x90;\n"
                              "sbit P1_1 = 0x91;\n"
                              "sbit P1_2 = 0x92;\n"
                              "sbit P1_3 = 0x93;\n"
                              "sbit P1_4 = 0x94;\n"
                              "sbit P1_5 = 0x95;\n"
                              "sbit P1_6 = 0x96;\n"
                              "sbit P1_7 = 0x97;\n\n"
                              "/*------------------------------------------------\n"
                              "TCON Bit Registers\n"
                              "------------------------------------------------*/\n"
                              "sbit IT0  = 0x88;\n"
                              "sbit IE0  = 0x89;\n"
                              "sbit IT1  = 0x8A;\n"
                              "sbit IE1  = 0x8B;\n"
                              "sbit TR0  = 0x8C;\n"
                              "sbit TF0  = 0x8D;\n"
                              "sbit TR1  = 0x8E;\n"
                              "sbit TF1  = 0x8F;\n\n"
                              "/*------------------------------------------------\n"
                              "IE Bit Registers\n"
                              "------------------------------------------------*/\n"
                              "sbit EX0  = 0xA8;\n"
                              "sbit ET0  = 0xA9;\n"
                              "sbit EX1  = 0xAA;\n"
                              "sbit ET1  = 0xAB;\n"
                              "sbit ES   = 0xAC;\n"
                              "sbit EA   = 0xAF;\n\n"
                              "#endif\n")
              ;; Если другие заголовочные файлы, можно добавить здесь
              (str "/* Заглушка для файла " filename " */\n"))))
        (catch Exception e
          (println (str "Ошибка включения файла: " filename))
          (str "/* Ошибка включения файла: " filename " */\n"))))))

;; Функция для замены макросов в строке
(defn replace-macros [line]
  (reduce (fn [s [macro value]]
            (str/replace s (re-pattern (str "\\b" macro "\\b")) value))
          line
          @macros))

;; Функция для обработки продолжения строки (\)
(defn process-line-continuation [lines]
  (loop [result []
         current ""
         [line & rest] lines]
    (if (nil? line)
      (if (not (empty? current))
        (conj result current)
        result)
      (let [trimmed (str/trim line)]
        (if (str/ends-with? trimmed "\\")
          (recur result
                 (str current (str/trimr (subs trimmed 0 (dec (count trimmed)))))
                 rest)
          (recur (conj result (str current trimmed))
                 ""
                 rest))))))

;; Основная функция препроцессора
(defn preprocess [input]
  (let [lines (str/split-lines input)
        processed-lines (atom [])
        skip-mode (atom false)]
    
    (doseq [line lines]
      (let [trimmed (str/trim line)]
        (cond
          ;; Пропуск строк в режиме условной компиляции
          @skip-mode
          (when (str/starts-with? trimmed "#endif")
            (reset! skip-mode false))
          
          ;; Обработка директив препроцессора
          (str/starts-with? trimmed "#")
          (let [directive (second (re-find #"#(\w+)" trimmed))]
            (case directive
              "define" (let [result (process-define trimmed)]
                        (when result (swap! processed-lines conj result)))
              "undef" (let [result (process-undef trimmed)]
                        (when result (swap! processed-lines conj result)))
              "ifdef" (let [result (process-ifdef trimmed)]
                        (when (nil? result) (reset! skip-mode true)))
              "ifndef" (let [result (process-ifndef trimmed)]
                         (when (nil? result) (reset! skip-mode true)))
              "endif" (let [result (process-endif trimmed)]
                        (when result (swap! processed-lines conj result)))
              "include" (let [result (process-include trimmed)]
                         (when result (swap! processed-lines conj result)))
              ;; Игнорируем неизвестные директивы
              nil))
          
          ;; Обработка обычных строк
          :else
          (let [processed (replace-macros trimmed)]
            (when (not (empty? processed))
              (swap! processed-lines conj processed))))))
    
    (-> @processed-lines
        process-line-continuation
        (str/join "\n"))))

;; Функция для тестирования препроцессора
(defn test-preprocessor []
  (let [test-code "#define LED_PORT P1
#define LED_PIN 0x01

interrupt void timer0_isr() {
    LED_PORT ^= LED_PIN;  // Инвертируем бит
}

#undef LED_PIN
#define LED_PIN 0x02

#ifdef DEBUG
    // Отладочный код
#endif

#ifndef RELEASE
    // Код для отладки
#endif"]
    (println "Исходный код:")
    (println test-code)
    (println "\nОбработанный код:")
    (println (preprocess test-code)))) 