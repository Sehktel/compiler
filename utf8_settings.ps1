# Установка кодировки UTF-8 для PowerShell
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::InputEncoding = [System.Text.Encoding]::UTF8
$PSDefaultParameterValues['*:Encoding'] = 'utf8'
$OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001

# Вывод сообщения о применении настроек
Write-Host "Настройки кодировки UTF-8 применены" -ForegroundColor Green 