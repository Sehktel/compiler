(ns graph-renderer.core
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import (java.io File)
           (java.nio.file Files Paths)
           (java.nio.file.attribute FileAttribute)))

;; Вспомогательные функции для работы с файловой системой
(defn ensure-dir! [path]
  "Создает директорию, если она не существует"
  (let [dir (io/file path)]
    (when-not (.exists dir)
      (.mkdirs dir))))

;; Функция для извлечения DOT-блоков из Markdown
(defn extract-dot-blocks [content]
  "Извлекает блоки DOT из Markdown-файла"
  (let [dot-pattern #"```dot\n(.*?)\n```"
        matcher (re-seq dot-pattern content :multiline)]
    (map second matcher)))

;; Функция для генерации уникального имени файла
(defn generate-unique-filename [base-path]
  "Генерирует уникальное имя файла с использованием временной метки"
  (let [timestamp (System/currentTimeMillis)
        filename (format "%s_%d.dot" base-path timestamp)]
    filename))

;; Функция для сохранения DOT-графа во временный файл
(defn save-dot-graph [dot-content output-path]
  "Сохраняет DOT-граф во временный файл"
  (spit output-path dot-content)
  output-path)

;; Функция для рендеринга DOT-графа в PNG
(defn render-dot-to-png [dot-file output-png]
  "Рендерит DOT-граф в PNG с использованием Graphviz"
  (let [process (-> (ProcessBuilder. 
                     ["dot" "-Tpng" dot-file "-o" output-png])
                    .inheritIO
                    .start)]
    (.waitFor process)
    (when (zero? (.exitValue process))
      output-png)))

;; Функция для обновления ссылок в Markdown
(defn update-markdown-links [markdown-content img-dir]
  "Обновляет ссылки на DOT-графы в Markdown"
  (let [dot-pattern #"```dot\n(.*?)\n```"
        replacement-fn (fn [match]
                         (let [dot-content (second match)
                               dot-file (generate-unique-filename (str img-dir "/graph"))
                               png-file (str/replace dot-file #"\.dot$" ".png")
                               dot-path (save-dot-graph dot-content dot-file)
                               png-path (render-dot-to-png dot-path png-file)]
                           (if png-path
                             (format "![Graph](%s)" 
                                     (str/replace png-path #"^\./" ""))
                             match)))]
    (str/replace markdown-content dot-pattern replacement-fn)))

;; Основная функция обработки файлов
(defn process-markdown-files [input-dir output-dir]
  "Обрабатывает все Markdown-файлы в указанной директории"
  (ensure-dir! output-dir)
  (ensure-dir! (str output-dir "/img"))
  
  (doseq [md-file (->> (io/file input-dir)
                       file-seq
                       (filter #(str/ends-with? (.getName %) ".md")))]
    (let [content (slurp md-file)
          updated-content (update-markdown-links content (str output-dir "/img"))
          output-file (io/file output-dir (.getName md-file))]
      (spit output-file updated-content)
      (println "Обработан файл:" (.getName md-file)))))

;; Точка входа
(defn -main [& args]
  (let [input-dir (or (first args) ".")
        output-dir (or (second args) "output")]
    (process-markdown-files input-dir output-dir)
    (println "Обработка завершена!")))

;; Для REPL или прямого запуска
(comment
  (process-markdown-files "." "output")) 