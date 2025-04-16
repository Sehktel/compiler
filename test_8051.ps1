# Скрипт для тестирования компилятора с C-кодом для микроконтроллера 8051
Write-Host "Тестирование компилятора с кодом для 8051 микроконтроллера" -ForegroundColor Cyan

# Убедиться, что тестовая директория существует
if (-not (Test-Path "test/c4ast")) {
    New-Item -ItemType Directory -Path "test/c4ast" -Force | Out-Null
    Write-Host "Создана директория для тестовых файлов: test/c4ast" -ForegroundColor Yellow
}

# Запустить тестирование 
Write-Host "Запуск теста 8051 кода..." -ForegroundColor Green
lein test-8051

# Функция для запуска с профилем test-8051-code должна быть добавлена в deps.edn
# Если вы используете Leiningen, скрипт должен быть изменен на:
# lein with-profile test-8051 run 