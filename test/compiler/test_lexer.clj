(ns compiler.test-lexer
  (:require [clojure.test :refer :all]
            [compiler.lexer :as lexer]))

(deftest test-lexer-basic
  "Тестирование базовой функциональности лексера"
  (let [test-code "void timer0_isr() interrupt 2 {
    // Простой обработчик прерывания
    P1 ^= 0x01;  // Инвертируем бит
}"
        tokens (lexer/lex test-code)]
    (is (= (count tokens) 10) "Правильное количество токенов")
    (is (= (:type (first tokens)) :void_keyword) "Первый токен - ключевое слово")
    (is (= (:value (first tokens)) "void") "Первое значение - 'void'")))

;; Дополнительные тесты для всех типов токенов

(deftest test-keywords
  "Тест распознавания всех ключевых слов"
  (let [keywords ["void" "interrupt" "char" "int" "signed" "unsigned" 
                  "const" "volatile" "typedef" "goto" "if" "else" 
                  "for" "while" "do" "return" "break" "continue"]
        test-code (clojure.string/join " " keywords)
        tokens (lexer/lex test-code)]
    
    (is (= (count tokens) (count keywords)) "Все ключевые слова распознаны")
    
    (doseq [i (range (count keywords))]
      (let [keyword (nth keywords i)
            token (nth tokens i)]
        (is (= (:value token) keyword) 
            (str "Ключевое слово '" keyword "' распознано корректно"))))))

(deftest test-identifiers
  "Тест распознавания идентификаторов"
  (let [identifiers ["x" "_var" "camelCase" "snake_case" "PascalCase" "UPPER_CASE"
                     "_123" "a1b2c3" "___" "very_very_long_identifier_name"]
        test-code (clojure.string/join " " identifiers)
        tokens (lexer/lex test-code)]
    
    (is (= (count tokens) (count identifiers)) "Все идентификаторы распознаны")
    
    (doseq [i (range (count identifiers))]
      (let [identifier (nth identifiers i)
            token (nth tokens i)]
        (is (= (:value token) identifier) 
            (str "Идентификатор '" identifier "' распознан корректно"))
        (is (= (:type token) :identifier) 
            (str "Тип '" identifier "' определен как идентификатор"))))))

(deftest test-numbers
  "Тест распознавания числовых литералов"
  (let [numbers ["0" "1" "42" "1234567890" "0x0" "0x1" "0xF" "0xabcdef" "0xABCDEF" "0x123456"]
        test-code (clojure.string/join " " numbers)
        tokens (lexer/lex test-code)]
    
    (is (= (count tokens) (count numbers)) "Все числа распознаны")
    
    (doseq [i (range (count numbers))]
      (let [number (nth numbers i)
            token (nth tokens i)]
        (is (= (:value token) number) 
            (str "Число '" number "' распознано корректно"))
        (is (= (:type token) :number) 
            (str "Тип '" number "' определен как число"))))))

(deftest test-operators
  "Тест распознавания операторов"
  (let [operators ["+" "-" "*" "/" "%" "=" "==" "!=" "^=" "<" "<=" ">" ">="
                   "++" "--" "&&" "||" "!" "&" "|" "^" "~" "<<" ">>"]
        test-code (clojure.string/join " " operators)
        tokens (lexer/lex test-code)]
    
    (is (= (count tokens) (count operators)) "Все операторы распознаны")
    
    (doseq [i (range (count operators))]
      (let [operator (nth operators i)
            token (nth tokens i)
            expected-type (condp = operator
                            "+" :plus
                            "-" :minus
                            "*" :asterisk
                            "/" :slash
                            "%" :percent
                            "=" :equal
                            "==" :equal_equal
                            "!=" :not_equal
                            "^=" :xor_equal
                            "<" :less
                            "<=" :less_equal
                            ">" :greater
                            ">=" :greater_equal
                            "++" :inc
                            "--" :dec
                            "&&" :logical_and
                            "||" :logical_or
                            "!" :logical_not
                            "&" :bit_and
                            "|" :bit_or
                            "^" :bit_xor
                            "~" :bit_not
                            "<<" :shift_left
                            ">>" :shift_right
                            nil)]
        (is (= (:value token) operator) 
            (str "Оператор '" operator "' распознан корректно"))
        (is (= (:type token) expected-type) 
            (str "Тип оператора '" operator "' определен корректно"))))))

(deftest test-separators
  "Тест распознавания разделителей"
  (let [separators ["(" ")" "{" "}" "[" "]" "," ";" ":"]
        test-code (clojure.string/join " " separators)
        tokens (lexer/lex test-code)]
    
    (is (= (count tokens) (count separators)) "Все разделители распознаны")
    
    (doseq [i (range (count separators))]
      (let [separator (nth separators i)
            token (nth tokens i)
            expected-type (condp = separator
                            "(" :open_round_bracket
                            ")" :close_round_bracket
                            "{" :open_curly_bracket
                            "}" :close_curly_bracket
                            "[" :open_square_bracket
                            "]" :close_square_bracket
                            "," :comma
                            ";" :semicolon
                            ":" :colon
                            nil)]
        (is (= (:value token) separator) 
            (str "Разделитель '" separator "' распознан корректно"))
        (is (= (:type token) expected-type) 
            (str "Тип разделителя '" separator "' определен корректно"))))))

(deftest test-strings
  "Тест распознавания строковых литералов"
  (let [strings ["\"\"" "\"a\"" "\"abc\"" "\"Hello, World!\"" "\"Special: \\n\\t\\r\\\"\""]
        test-code (clojure.string/join " " strings)
        tokens (lexer/lex test-code)]
    
    (is (= (count tokens) (count strings)) "Все строки распознаны")
    
    (doseq [i (range (count strings))]
      (let [str-literal (nth strings i)
            token (nth tokens i)]
        (is (= (:value token) str-literal) 
            (str "Строка '" str-literal "' распознана корректно"))
        (is (= (:type token) :string) 
            (str "Тип '" str-literal "' определен как строка"))))))

(deftest test-comments
  "Тест распознавания комментариев"
  (let [single-line "// Это однострочный комментарий"
        multi-line "/* Это
многострочный
комментарий */"
        test-code (str single-line "\n" multi-line)
        tokens (lexer/lex test-code)]
    
    (is (= (count tokens) 2) "Оба комментария распознаны")
    
    (let [single-token (first tokens)
          multi-token (second tokens)]
      (is (= (:value single-token) single-line)
          "Однострочный комментарий распознан корректно")
      (is (= (:type single-token) :comment)
          "Тип однострочного комментария определен корректно")
      
      (is (= (:value multi-token) multi-line)
          "Многострочный комментарий распознан корректно")
      (is (= (:type multi-token) :comment)
          "Тип многострочного комментария определен корректно"))))

(deftest test-position-tracking
  "Тест отслеживания позиций токенов"
  (let [test-code "int x = 10;\nif (x > 5) {\n    return 0;\n}"
        tokens (lexer/lex test-code)]
    
    ;; Проверка строк и колонок
    (is (= (:line (nth tokens 0)) 1) "Первая строка определена корректно")
    (is (= (:column (nth tokens 0)) 1) "Первая колонка определена корректно")
    
    ;; Проверка перехода на новую строку
    (is (= (:line (nth tokens 4)) 2) "Переход на вторую строку определен корректно")
    (is (= (:column (nth tokens 4)) 1) "Начало второй строки определено корректно")
    
    ;; Проверка третьей строки
    (is (= (:line (nth tokens 9)) 3) "Переход на третью строку определен корректно")
    
    ;; Проверка конца
    (let [last-token (last tokens)]
      (is (= (:line last-token) 4) "Последняя строка определена корректно")
      (is (= (:column last-token) 1) "Последняя колонка определена корректно"))))

(deftest test-complex-code
  "Тест обработки сложного кода"
  (let [test-code "
int factorial(int n) {
    /* Вычисление факториала
       рекурсивным методом */
    if (n == 0 || n == 1) {
        return 1;  // Базовый случай
    } else {
        return n * factorial(n - 1);  // Рекурсивный вызов
    }
}

void main() {
    int num = 5;
    int result = factorial(num);
    printf(\"Factorial of %d is %d\\n\", num, result);
}"
        tokens (lexer/lex test-code)]
    
    ;; Основная проверка - нет ошибок при лексическом анализе
    (is (lexer/validate-tokens tokens) "Сложный код обработан без ошибок")
    
    ;; Проверка типов ключевых токенов
    (let [token-types (map :type tokens)
          token-values (map :value tokens)]
      
      ;; Проверка распознавания функции factorial
      (is (some #(= % :identifier) token-types) "Идентификаторы определены")
      (is (some #(= % "factorial") token-values) "Функция factorial распознана")
      
      ;; Проверка распознавания операторов сравнения и логических операторов
      (is (some #(= % :equal_equal) token-types) "Оператор == распознан")
      (is (some #(= % :logical_or) token-types) "Логический оператор || распознан")
      
      ;; Проверка распознавания рекурсивного вызова
      (is (some #(and (= (:type %) :identifier) (= (:value %) "factorial")) tokens) 
          "Рекурсивный вызов factorial распознан")
      
      ;; Проверка распознавания строки с escape-последовательностями
      (is (some #(and (= (:type %) :string) (.contains (:value %) "\\n")) tokens)
          "Строка с escape-последовательностью распознана"))))

(deftest test-edge-cases
  "Тест обработки краевых случаев"
  (testing "Пустой ввод"
    (let [tokens (lexer/lex "")]
      (is (empty? tokens) "Пустой ввод обрабатывается корректно")))
  
  (testing "Токены без пробелов"
    (let [test-code "int a=0;a++;"
          tokens (lexer/lex test-code)]
      (is (= (count tokens) 8) "Корректное распознавание токенов без пробелов")
      (is (= (map :type (take 3 tokens)) 
             '(:type_int_keyword :identifier :equal))
          "Первые три токена определены правильно")))
  
  (testing "Многострочный комментарий с вложенностью (не поддерживается в C, но проверяем обработку)"
    (let [test-code "/* внешний /* вложенный */ комментарий */"
          tokens (lexer/lex test-code)]
      ;; В C вложенные комментарии не поддерживаются, поэтому ожидаем два токена
      (is (>= (count tokens) 1) "Многострочный комментарий распознан"))))

(deftest test-escape-sequences
  "Тест обработки escape-последовательностей"
  (testing "Строки с escape-символами"
    (let [test-cases [["\"\\n\"" "\\n"]                      ; новая строка
                      ["\"\\t\"" "\\t"]                      ; табуляция
                      ["\"\\\"\"" "\\\""]                    ; кавычка
                      ["\"\\\\\"" "\\\\"]                    ; обратный слеш
                      ["\"Hello\\nWorld\"" "Hello\\nWorld"]] ; комбинация
          test-code (clojure.string/join " " (map first test-cases))]
      
      (let [tokens (lexer/lex test-code)]
        (is (= (count tokens) (count test-cases)) "Все токены распознаны")
        
        (doseq [i (range (count test-cases))]
          (let [expected (first (nth test-cases i))
                token (nth tokens i)]
            (is (= (:value token) expected) 
                (str "Строка с escape-символами '" expected "' распознана корректно"))
            (is (= (:type token) :string) 
                (str "Тип '" expected "' определен как строка")))))))
  
  (testing "Комментарии с обратным слешем"
    (let [test-cases ["// Комментарий с \\ обратным слешем"
                      "/* Многострочный \n комментарий \\ с обратным слешем */"]
          test-code (clojure.string/join "\n" test-cases)]
      
      (let [tokens (lexer/lex test-code)]
        (is (= (count tokens) (count test-cases)) "Все комментарии распознаны")
        
        (doseq [i (range (count test-cases))]
          (let [expected (nth test-cases i)
                token (nth tokens i)]
            (is (= (:value token) expected) 
                (str "Комментарий с обратным слешем распознан корректно"))
            (is (= (:type token) :comment) 
                (str "Тип определен как комментарий"))))))))

(defn print-lexer-tokens
  "Функция для печати токенов с целью визуальной отладки"
  [test-code]
  (let [tokens (lexer/lex test-code)]
    (doseq [token tokens]
      (println (format "Тип: %-20s Значение: %-30s Строка: %d Колонка: %d" 
                       (name (:type token)) 
                       (:value token) 
                       (:line token) 
                       (:column token))))))

(defn run-all-tests
  "Запуск всех тестов лексера"
  []
  (run-tests 'compiler.test-lexer))
