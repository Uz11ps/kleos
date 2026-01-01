# Инструкция: Обновление сервера после git pull

## После успешного git pull

### 1. Перейдите в папку server

```bash
cd /opt/kleos/kleos/server
```

### 2. Установите зависимости (если обновились)

```bash
npm install
```

### 3. Перезапустите PM2

```bash
pm2 restart kleos-api
```

### 4. Проверьте логи

```bash
pm2 logs kleos-api --lines 50
```

### 5. Проверьте статус

```bash
pm2 status
```

## Если нужно пересобрать TypeScript

```bash
cd /opt/kleos/kleos/server
npm run build
pm2 restart kleos-api
```

## Проверка работы сервера

```bash
# Проверьте, что сервер отвечает
curl http://localhost:3000/api/health
# или
curl https://api.kleos-study.ru/api/health
```

