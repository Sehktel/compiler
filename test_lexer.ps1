# Скрипт для тестирования лексера с C-кодом для микроконтроллера 8051
Write-Host "Тестирование лексера с кодом для 8051 микроконтроллера" -ForegroundColor Cyan

# Получаем путь к файлу, который нужно протестировать
$testFile = "test/test_file.c"
if ($args.Count -gt 0) {
    $testFile = $args[0]
}

# Запускаем REPL и вручную тестируем
Write-Host "Запуск REPL для тестирования файла: $testFile" -ForegroundColor Green
Write-Host "Вы можете использовать следующие команды:" -ForegroundColor Yellow
Write-Host "(require '[compiler.lexer :refer [tokenize]])" -ForegroundColor Yellow
Write-Host "(require '[compiler.pre_processor :refer [preprocess]])" -ForegroundColor Yellow
Write-Host "(def code (slurp \"$testFile\"))" -ForegroundColor Yellow
Write-Host "(def processed (preprocess code))" -ForegroundColor Yellow
Write-Host "(def tokens (tokenize processed))" -ForegroundColor Yellow
Write-Host "(doseq [token tokens] (println token))" -ForegroundColor Yellow

# Запускаем REPL
lein repl 