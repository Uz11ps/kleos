# Диагностика почтового сервиса

## Быстрая проверка SMTP подключения

На сервере выполните:

```bash
curl https://api.kleos-study.ru/smtp/ping
```

Или откройте в браузере:
```
https://api.kleos-study.ru/smtp/ping
```

**Ожидаемый результат при успешной настройке:**
```json
{
  "ok": true,
  "host": "smtp.example.com",
  "portTried": 587,
  "secureTried": false,
  "proxyUsed": "disabled"
}
```

**Если SMTP не настроен:**
```json
{
  "ok": false,
  "error": "smtp_not_configured"
}
```

**Если есть проблемы с подключением:**
```json
{
  "ok": false,
  "error": "описание ошибки",
  "code": "код ошибки"
}
```

## Проверка переменных окружения

На сервере проверьте наличие переменных окружения:

```bash
cd /opt/kleos/kleos/server
pm2 env 0 | grep SMTP
```

Или проверьте файл `.env` (если используется):

```bash
cat .env | grep SMTP
```

### Необходимые переменные окружения:

1. **SMTP_HOST** - адрес SMTP сервера (например: `smtp.gmail.com`, `smtp.mail.ru`)
2. **SMTP_PORT** - порт SMTP (обычно: `587` для TLS, `465` для SSL, `25` для без шифрования)
3. **SMTP_SECURE** - использовать SSL (`true` для порта 465, `false` для порта 587)
4. **SMTP_USER** - имя пользователя для авторизации
5. **SMTP_PASS** - пароль для авторизации
6. **SMTP_FROM** - адрес отправителя (например: `no-reply@kleos-study.ru`)
7. **SMTP_TLS_INSECURE** - отключить проверку TLS сертификата (только для тестирования, `true`/`false`)

### Пример настройки для Gmail:

```bash
export SMTP_HOST="smtp.gmail.com"
export SMTP_PORT="587"
export SMTP_SECURE="false"
export SMTP_USER="your-email@gmail.com"
export SMTP_PASS="your-app-password"  # Используйте пароль приложения, не обычный пароль!
export SMTP_FROM="your-email@gmail.com"
```

### Пример настройки для Mail.ru:

```bash
export SMTP_HOST="smtp.mail.ru"
export SMTP_PORT="465"
export SMTP_SECURE="true"
export SMTP_USER="your-email@mail.ru"
export SMTP_PASS="your-password"
export SMTP_FROM="your-email@mail.ru"
```

### Пример настройки для Yandex:

```bash
export SMTP_HOST="smtp.yandex.ru"
export SMTP_PORT="465"
export SMTP_SECURE="true"
export SMTP_USER="your-email@yandex.ru"
export SMTP_PASS="your-password"
export SMTP_FROM="your-email@yandex.ru"
```

## Проверка логов отправки email

Проверьте логи PM2 на наличие ошибок отправки:

```bash
pm2 logs kleos-api --lines 100 | grep -i "email\|smtp\|verification"
```

Ищите сообщения типа:
- `Verification email queued to ...` - email поставлен в очередь
- `Send verification email failed` - ошибка отправки
- `SMTP send failed` - ошибка SMTP

## Альтернатива: HTTP Relay

Если SMTP не работает, можно использовать HTTP relay через переменную:

```bash
export EMAIL_RELAY_URL="https://your-relay-service.com/api/send-email"
```

Relay должен принимать POST запрос с JSON:
```json
{
  "to": "user@example.com",
  "name": "Имя пользователя",
  "subject": "Kleos — подтверждение email",
  "html": "<html>...</html>",
  "fromEmail": "no-reply@kleos-study.ru",
  "fromName": "Kleos University"
}
```

## Тестовая отправка email

Для тестовой отправки можно временно добавить тестовый эндпоинт или использовать существующий функционал регистрации.

## Частые проблемы

1. **Gmail требует пароль приложения** - не используйте обычный пароль, создайте пароль приложения в настройках аккаунта Google
2. **Порт заблокирован** - некоторые провайдеры блокируют порты 25, 587, 465. Попробуйте использовать прокси через `SMTP_PROXY`
3. **TLS сертификат** - если сервер использует самоподписанный сертификат, установите `SMTP_TLS_INSECURE=true`
4. **Firewall** - убедитесь, что сервер может подключиться к SMTP серверу

## Настройка переменных окружения в PM2

После настройки переменных окружения перезапустите PM2:

```bash
pm2 restart kleos-api --update-env
```

Или добавьте переменные в ecosystem файл PM2.

