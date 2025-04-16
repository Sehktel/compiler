# Компилятор языка C (Учебный проект)

Данный проект представляет собой реализацию компилятора языка C-подобного языка для учебных целей.

## Структура проекта

- `src/compiler/lexer.clj` - лексический анализатор
- `src/compiler/parser.clj` - синтаксический анализатор
- `src/compiler/ast.clj` - абстрактное синтаксическое дерево
- `test/compiler/test_lexer.clj` - тесты для лексического анализатора

## Установка зависимостей

### Leiningen
```bash
lein deps
```

### Clojure CLI
```bash
clj -P
```

## Запуск и тестирование

### Тестирование AST

```bash
# Запуск тестов AST
lein ast-test

# Парсинг конкретного файла
lein parse-file simple_arithmetic.c
```

### Запуск тестов

```bash
# Запуск всех тестов
lein test

# Запуск тестов с обновлением
lein test-refresh

# Генерация отчета о покрытии кода
lein coverage
```

### Статический анализ кода

```bash
# Статический анализ с помощью Kibit
lein lint
```

## Интерактивная разработка

### REPL

```bash
# Запуск REPL
lein repl

# В REPL можно использовать:
(test-ast "simple_arithmetic.c")
(parse-file "control_flow.c")
```

## Примеры использования

### Лексический анализ

```clojure
(require '[compiler.lexer :as lexer])

;; Анализ кода
(def tokens (lexer/lex "int main() { return 0; }"))

;; Печать токенов
(lexer/print-lexer-tokens "int x = 10; // комментарий")
```

### AST Visualization

```clojure
;; В REPL или через lein ast-test
(test-ast "simple_arithmetic.c")
```

## Возможности

- Лексический анализ с поддержкой escape-символов
- Парсинг основных конструкций C
- Визуализация Abstract Syntax Tree
- Статический анализ кода

## Требования

- Clojure 1.11.1+
- Leiningen 2.9.0+

## Лицензия

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

# C Compiler Project: AST Visualization and Testing

## AST Visualization

### Running AST Tests

1. Open the REPL
2. Load the `user` namespace
3. Use the following functions:

```clojure
;; Test a specific file
(test-ast "simple_arithmetic.c")
(test-ast "control_flow.c")

;; Parse a file and get the AST
(def arithmetic-ast (parse-file "simple_arithmetic.c"))

;; Print a specific AST
(ast/print-ast arithmetic-ast)
```

### Adding New Test Files

1. Place C source files in `test/resources/c_sources/`
2. Use the `test-ast` or `parse-file` functions to test them

## Example AST Visualization

### Simple Arithmetic Example

```c
int main() {
    int x = 10;
    int y = 20;
    int z = x + y * 2;
    return z;
}
```

This will generate an AST showing:
- Main function declaration
- Variable declarations
- Binary operations
- Return statement

### Control Flow Example

```c
int factorial(int n) {
    if (n <= 1) {
        return 1;
    } else {
        return n * factorial(n - 1);
    }
}
```

This will demonstrate:
- Function declaration
- Recursive function call
- Conditional (if-else) structure
- Comparison and multiplication operations

## Troubleshooting

- Ensure all dependencies are installed
- Check that the parser can handle the specific C constructs in your test files
- Use `*debug-mode*` in the parser for additional logging

## Тестирование и демонстрация

### Специализированные тесты

```bash
# Тесты лексического анализатора
lein test-lexer

# Тесты парсера
lein test-parser

# Тесты AST
lein test-ast
```

### Демонстрационные команды

```bash
# Демонстрация лексического анализа
lein demo-lexer

# Демонстрация парсинга
lein demo-parser

# Демонстрация печати AST
lein demo-ast-print
```

### Дополнительные инструменты

```bash
# Статический анализ кода
lein lint

# Генерация отчета о покрытии кода
lein coverage
```

## Документация

### Генерация документации

```bash
# Генерация документации с помощью Codox
lein docs
```

#### Особенности документации
- Автоматическая генерация из docstrings
- Поддержка Markdown в комментариях
- Привязка к исходному коду на GitHub
- Покрытие основных неймспейсов:
  - `compiler.lexer`
  - `compiler.parser`
  - `compiler.ast`

### Просмотр документации

1. После генерации откройте `docs/index.html`
2. Документация содержит:
   - Описание неймспейсов
   - Документацию функций
   - Примеры использования
   - Ссылки на исходный код

### Требования к документации

- Использование docstrings с форматом Markdown
- Описание параметров и возвращаемых значений
- Примеры использования функций

