# Компилятор языка C (Учебный проект)

Данный проект представляет собой реализацию компилятора языка C-подобного языка для учебных целей.

## Структура проекта

- `src/compiler/lexer.clj` - лексический анализатор
- `src/compiler/parser.clj` - синтаксический анализатор
- `src/compiler/ast.clj` - абстрактное синтаксическое дерево
- `test/compiler/test_lexer.clj` - тесты для лексического анализатора

## Запуск

### Используя Clojure CLI (deps.edn)

```bash
# Запуск демонстрации работы лексического анализатора
clj -M -e "(require 'compiler.lexer) (compiler.lexer/-main)"

# Запуск тестов
clj -M:test -m cognitect.test-runner
```

### Используя Leiningen

```bash
# Запуск демонстрации работы лексического анализатора
lein run -m compiler.lexer

# Запуск тестов
lein test
```

## Обработка escape-символов

В проекте реализована обработка escape-символов и обратных слешей в строках и комментариях:

- Строки: `"Hello\nWorld"`, `"Quoted \"text\""`, `"Path: C:\\Windows\\"`
- Комментарии: `// Комментарий с \ обратным слешем`, `/* Многострочный комментарий с \ слешем */`

## Лексический анализатор

Лексический анализатор реализован с использованием детерминированного конечного автомата (DFA). Он выполняет следующие функции:

1. Распознавание всех типов токенов C-подобного языка:
   - Ключевые слова (`if`, `while`, `return`, и т.д.)
   - Операторы (`+`, `-`, `*`, `/`, `==`, и т.д.)
   - Разделители (скобки, запятые, точки с запятой)
   - Идентификаторы
   - Числовые литералы (десятичные и шестнадцатеричные)
   - Строковые литералы
   - Комментарии (однострочные и многострочные)

2. Отслеживание позиции токенов в исходном коде (строка и колонка)

3. Обработка escape-символов в строках и комментариях

## Пример использования API

```clojure
(require '[compiler.lexer :as lexer])

;; Анализ кода
(def tokens (lexer/lex "int main() { return 0; }"))

;; Преобразование токенов для парсера
(def parser-tokens (lexer/tokenize "int main() { return 0; }"))

;; Печать информации о токенах
(lexer/print-lexer-tokens "int x = 10; // комментарий")
```

## Тестирование

Тесты охватывают все аспекты лексического анализатора:
- Распознавание всех типов токенов
- Корректная обработка строк и комментариев
- Обработка escape-символов
- Правильное отслеживание позиций
- Обработка сложных и краевых случаев

## License

MIT License

Copyright (c) 2025 Sehktel

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
