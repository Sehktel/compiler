(ns pre-processor
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
        (slurp filename)
        (catch Exception e
          (println (str "Ошибка включения файла: " filename))
          "")))))

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