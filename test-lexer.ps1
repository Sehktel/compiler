# Скрипт для тестирования лексера с C-кодом для микроконтроллера 8051
Write-Host "Тестирование лексера с кодом для 8051 микроконтроллера" -ForegroundColor Cyan

# Получаем путь к файлу, который нужно протестировать
$testFile = "test/test_file.c"
if ($args.Count -gt 0) {
    $testFile = $args[0]
}

# Запускаем тест
Write-Host "Запуск теста для файла: $testFile" -ForegroundColor Green
clojure -m test-8051-lexer $testFile 