# Инструкция: Как обновить проект на сервере через git pull

## Проблема
Вы находитесь в домашней директории (`~`), а не в папке с проектом.

## Решение

### 1. Найдите папку с проектом

Обычно проект находится в одной из этих папок:

```bash
# Проверьте, где находится ваш проект
ls -la ~/
ls -la /var/www/
ls -la /opt/
ls -la /home/

# Или найдите папку с server
find ~ -name "server" -type d 2>/dev/null
find / -name "package.json" -path "*/server/*" 2>/dev/null | head -5
```

### 2. Перейдите в папку проекта

Если проект находится, например, в `/var/www/kleos` или `/opt/kleos`:

```bash
cd /var/www/kleos
# или
cd /opt/kleos
# или другая папка, где находится ваш проект
```

### 3. Проверьте, что это git репозиторий

```bash
# Должна быть папка .git
ls -la | grep .git

# Или проверьте статус
git status
```

### 4. Сделайте git pull

```bash
# Получите последние изменения
git pull origin main

# Или если ветка называется master
git pull origin master
```

### 5. Установите зависимости (если нужно)

```bash
# Если обновились зависимости в package.json
cd server
npm install
```

### 6. Перезапустите приложение через PM2

```bash
# Перезапустите приложение
pm2 restart kleos-api

# Или если нужно перезагрузить все
pm2 restart all

# Проверьте статус
pm2 status
pm2 logs kleos-api --lines 50
```

## Быстрый способ найти проект

Если вы используете PM2, можно найти путь к проекту:

```bash
# Посмотрите конфигурацию PM2
pm2 show kleos-api

# В выводе будет поле "script path" - это путь к проекту
```

## Альтернативный способ: клонировать заново

Если не можете найти старую папку:

```bash
# Создайте папку для проекта
mkdir -p /var/www/kleos
cd /var/www/kleos

# Клонируйте репозиторий
git clone https://github.com/Uz11ps/kleos.git .

# Установите зависимости
cd server
npm install

# Настройте .env файл (если нужно)
cp .env.example .env
nano .env

# Запустите через PM2
pm2 start server/index.js --name kleos-api
pm2 save
```

## Проверка после обновления

```bash
# Проверьте логи
pm2 logs kleos-api --lines 100

# Проверьте, что сервер работает
curl http://localhost:3000/api/health
# или
curl https://api.kleos-study.ru/api/health
```

