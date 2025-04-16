(ns rename-namespaces
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

;; Научный комментарий: Функция для безопасного переименования пространства имен
;; Использует точный парсинг для минимизации побочных эффектов
(defn rename-namespace-in-file [file-path]
  (let [content (slurp file-path)
        ;; Точный регулярное выражение для поиска объявления пространства имен
        ns-pattern #"\(ns\s+([a-zA-Z0-9\-\.\/]+)"
        updated-content (str/replace content 
                                     ns-pattern 
                                     (fn [[full-match ns-name]]
                                       (let [;; Замена дефисов на подчеркивания только в имени пространства
                                             updated-ns (str/replace ns-name #"-" "_")]
                                         (str "(ns " updated-ns))))]
    (spit file-path updated-content)
    (println "Обновлено пространство имен в файле:" file-path)))

;; Научный комментарий: Функция для рекурсивного поиска и обработки Clojure-файлов
(defn process-clojure-files [root-path]
  (let [clojure-files (->> (io/file root-path)
                           file-seq
                           (filter #(and (.isFile %)
                                         (or (str/ends-with? (.getName %) ".clj")
                                             (str/ends-with? (.getName %) ".cljc")))))]
    (doseq [file clojure-files]
      (try
        (rename-namespace-in-file (.getPath file))
        (catch Exception e
          (println "Ошибка при обработке файла:" (.getPath file)
                   "Детали ошибки:" (.getMessage e)))))))

;; Точка входа для выполнения скрипта
(defn -main []
  (process-clojure-files ".")
  (println "Завершено переименование пространств имен.")) 