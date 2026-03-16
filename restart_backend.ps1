# Скрипт для перезапуска бэкенда на сервере
$server = "95.163.228.70"
$user = "root"
$password = "123"

Write-Host "Подключение к серверу $server..."

# Проверяем наличие модуля Posh-SSH
if (-not (Get-Module -ListAvailable -Name Posh-SSH)) {
    Write-Host "Установка модуля Posh-SSH..."
    Install-Module -Name Posh-SSH -Force -Scope CurrentUser -SkipPublisherCheck
}

Import-Module Posh-SSH

# Создаем credentials
$securePassword = ConvertTo-SecureString $password -AsPlainText -Force
$credential = New-Object System.Management.Automation.PSCredential($user, $securePassword)

try {
    # Подключаемся к серверу
    $session = New-SSHSession -ComputerName $server -Credential $credential -AcceptKey
    
    if ($session) {
        Write-Host "Подключение установлено!"
        
        # Находим папку с проектом
        Write-Host "Поиск папки server..."
        $findServer = Invoke-SSHCommand -SessionId $session.SessionId -Command "find /opt /var/www /home -name 'package.json' -path '*/server/*' -type f 2>/dev/null | head -1 | xargs dirname 2>/dev/null || echo '/opt/kleos/kleos/server'"
        $serverPath = $findServer.Output.Trim()
        Write-Host "Найдена папка: $serverPath"
        
        # Перезапускаем PM2
        Write-Host "Перезапуск PM2 процесса kleos-api..."
        $restart = Invoke-SSHCommand -SessionId $session.SessionId -Command "cd $serverPath && pm2 restart kleos-api 2>&1 || pm2 restart all 2>&1"
        Write-Host $restart.Output
        
        # Проверяем статус
        Write-Host "`nПроверка статуса PM2..."
        $status = Invoke-SSHCommand -SessionId $session.SessionId -Command "pm2 status"
        Write-Host $status.Output
        
        # Показываем последние логи
        Write-Host "`nПоследние логи (20 строк)..."
        $logs = Invoke-SSHCommand -SessionId $session.SessionId -Command "pm2 logs kleos-api --lines 20 --nostream 2>&1 || echo 'Логи недоступны'"
        Write-Host $logs.Output
        
        # Проверяем health endpoint
        Write-Host "`nПроверка health endpoint..."
        $health = Invoke-SSHCommand -SessionId $session.SessionId -Command "curl -s http://localhost:8080/api/health 2>&1 || curl -s http://localhost:3000/api/health 2>&1 || echo 'Health check failed'"
        Write-Host $health.Output
        
        # Закрываем сессию
        Remove-SSHSession -SessionId $session.SessionId | Out-Null
        Write-Host "`nГотово! Бэкенд перезапущен."
    } else {
        Write-Host "Ошибка подключения к серверу"
    }
} catch {
    Write-Host "Ошибка: $_"
    Write-Host "`nАльтернативный способ: выполните команды вручную через SSH:"
    Write-Host "ssh root@95.163.228.70"
    Write-Host "pm2 restart kleos-api"
    Write-Host "pm2 status"
}
