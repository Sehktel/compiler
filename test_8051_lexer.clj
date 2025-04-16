(ns test-8051-lexer
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(def token-types
  {:sfr_keyword ["sfr"]
   :sbit_keyword ["sbit"]
   :void_keyword ["void"]
   :interrupt_keyword ["interrupt"]
   :identifier #"[a-zA-Z_][a-zA-Z0-9_]*"
   :number #"0[xX][0-9a-fA-F]+|\d+"
   :equal ["="]
   :semicolon [";"]
   :open_round_bracket ["("]
   :close_round_bracket [")"]
   :open_curly_bracket ["{"]
   :close_curly_bracket ["}"]
   :comment #"//[^\n]*|/\*[\s\S]*?\*/"})

(defn is-keyword? [s]
  (some #(some (partial = s) %) 
        (vals (select-keys token-types 
                          [:sfr_keyword
                           :sbit_keyword
                           :void_keyword
                           :interrupt_keyword]))))

(defn separator? [s]
  (some #(some (partial = s) %) 
        (vals (select-keys token-types 
                          [:open_round_bracket 
                           :close_round_bracket 
                           :open_curly_bracket
                           :close_curly_bracket 
                           :semicolon]))))

(defn operator? [s]
  (some #(some (partial = s) %) 
        (vals (select-keys token-types 
                          [:equal]))))

(defn get-token-type [token]
  (cond
    (is-keyword? token) :keyword
    (operator? token) :operator
    (separator? token) :separator
    (re-matches (:identifier token-types) token) :identifier
    (re-matches (:number token-types) token) :number
    (re-matches (:comment token-types) token) :comment
    :else :unknown))

(defn find-exact-token-type [value]
  (or 
   (first 
    (filter 
     #(and (not (#{:identifier :number :comment} %))
           (let [token-val (get token-types %)]
             (if (vector? token-val)
               (some (partial = value) token-val)
               false)))
     (keys token-types)))
   
   (cond
     (re-matches (:identifier token-types) value) :identifier
     (re-matches (:number token-types) value) :number
     (re-matches (:comment token-types) value) :comment
     :else :unknown)))

(defn tokenize [code]
  (if (nil? code)
    []
    (let [lines (str/split-lines code)
          tokens (atom [])]
      (doseq [line lines]
        (let [words (-> line 
                      (str/replace #";" " ; ")
                      (str/replace #"=" " = ")
                      (str/replace #"\(" " ( ")
                      (str/replace #"\)" " ) ")
                      (str/replace #"\{" " { ")
                      (str/replace #"\}" " } ")
                      (str/split #"\s+"))]
          (doseq [word words]
            (when-not (empty? word)
              (let [token-type (find-exact-token-type word)]
                (swap! tokens conj [token-type word]))))))
      @tokens)))

(defn test-lexer [file-path]
  (println "Testing file:" file-path)
  (let [code (slurp file-path)]
    (println "\nSource code:")
    (println code)
    
    (println "\nTokens:")
    (let [tokens (tokenize code)]
      (doseq [token tokens]
        (println token))
      tokens)))

(defn -main [& args]
  (let [file-path (or (first args) "test/test_file.c")]
    (test-lexer file-path))) 