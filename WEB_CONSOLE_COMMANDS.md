# Команды для веб-консоли ISPManager

## Шаг 1: Откройте консоль
Нажмите **"Открыть консоль"** в меню сервера

## Шаг 2: Выполните команды по порядку

### 1. Проверка текущего статуса PM2
```bash
pm2 status
```

### 2. Перезапуск бэкенда
```bash
pm2 restart kleos-api
```

Если процесс называется по-другому, попробуйте:
```bash
pm2 restart all
```

### 3. Проверка статуса после перезапуска
```bash
pm2 status
```

### 4. Просмотр логов (последние 50 строк)
```bash
pm2 logs kleos-api --lines 50 --nostream
```

### 5. Проверка health endpoint
```bash
curl http://localhost:8080/health
```
или
```bash
curl http://localhost:3000/health
```

### 6. Проверка, на каком порту работает приложение
```bash
netstat -tuln | grep -E ':(3000|8080|80|443)'
```

## Дополнительные команды для диагностики

### Проверка SSH сервиса
```bash
systemctl status sshd
```
или
```bash
systemctl status ssh
```

### Проверка файрвола (если используется ufw)
```bash
ufw status
```

### Проверка файрвола (если используется firewalld)
```bash
firewall-cmd --list-all
```

### Проверка файрвола (если используется iptables)
```bash
iptables -L -n | grep 22
```

### Попытка открыть порт 22 через ufw (если установлен)
```bash
sudo ufw allow 22/tcp
sudo ufw reload
```

### Проверка, слушает ли SSH порт 22
```bash
netstat -tuln | grep 22
```
или
```bash
ss -tuln | grep 22
```

## Если нужно найти путь к проекту
```bash
find /opt /var/www /home -name 'package.json' -path '*/server/*' -type f 2>/dev/null | head -1 | xargs dirname 2>/dev/null
```

## Если нужно перейти в папку проекта и перезапустить
```bash
cd /opt/kleos/kleos/server 2>/dev/null || cd $(find /opt /var/www /home -name 'package.json' -path '*/server/*' -type f 2>/dev/null | head -1 | xargs dirname 2>/dev/null)
pm2 restart kleos-api
```
