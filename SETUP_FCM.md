# Настройка Firebase Cloud Messaging (FCM) на сервере

## Шаг 1: Загрузить Service Account JSON файл на сервер

### Вариант A: Через SCP (с локального компьютера)
```bash
scp app/src/main/java/com/example/kleos/kleos-8e95f-firebase-adminsdk-fbsvc-b5560a46ea.json root@ваш_сервер:/opt/kleos/kleos/server/firebase-service-account.json
```

### Вариант B: Через ispmanager файловый менеджер
1. Зайдите в ispmanager → Файловый менеджер
2. Перейдите в `/opt/kleos/kleos/server/`
3. Загрузите файл `kleos-8e95f-firebase-adminsdk-fbsvc-b5560a46ea.json`
4. Переименуйте его в `firebase-service-account.json`

### Вариант C: Создать файл напрямую на сервере через SSH
```bash
# Подключитесь к серверу
ssh root@ваш_сервер

# Перейдите в директорию проекта
cd /opt/kleos/kleos/server

# Создайте файл (скопируйте содержимое JSON файла)
nano firebase-service-account.json
# Вставьте содержимое JSON файла, сохраните (Ctrl+O, Enter, Ctrl+X)

# Установите права доступа
chmod 600 firebase-service-account.json
```

## Шаг 2: Добавить переменные в .env файл

```bash
# На сервере
cd /opt/kleos/kleos/server

# Откройте .env файл
nano .env

# Добавьте следующие строки в конец файла:
FCM_SERVICE_ACCOUNT_PATH=/opt/kleos/kleos/server/firebase-service-account.json
FCM_PROJECT_ID=kleos-8e95f

# Сохраните файл (Ctrl+O, Enter, Ctrl+X)
```

## Шаг 3: Перезапустить сервер

```bash
# Перезапустить PM2 процесс
pm2 restart kleos-api

# Или перезапустить все процессы
pm2 restart all

# Проверить статус
pm2 status

# Посмотреть логи
pm2 logs kleos-api --lines 50
```

## Шаг 4: Проверить работу

После перезапуска в логах должно появиться:
```
Loaded service account from file: /opt/kleos/kleos/server/firebase-service-account.json
```

## Альтернативный способ: Добавить JSON в переменную окружения

Если не хотите загружать файл, можно добавить JSON напрямую в .env:

```bash
cd /opt/kleos/kleos/server
nano .env

# Добавьте (замените содержимое на ваш JSON в одну строку):
FCM_SERVICE_ACCOUNT_JSON='{"type":"service_account","project_id":"kleos-8e95f","private_key_id":"b5560a46eae50cfbebacdbe73b3167a4d4dc4d86","private_key":"-----BEGIN PRIVATE KEY-----\nMIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCFaXfX18VtYbXx\n5wF/KY3wCl1uSSOQv2tpryxyzz3hWPhEUEQVPk5ZTjoWGczyspNL2of59r4dkQIv\ng/ZzPRAJeXmQ2g9aDd0PFVoObfMwD4SIcdXI81++gIgP56jZrbdHjccBlpijEz6A\nXWMJn+f3lAfm+yuE+kx9cRLp2k0Thj96Q24Mw75CtwTATnbKxknLYzs0V6wixXXr\n3NGGdOvmKa7Rf05JQwYN+VX1a+QhFIHH+xuVIGHRfs5nAEjyeUZ3qnT5nwG/wObb\nN1PsqLQEFJ8oozxSwZk9vXF9SZb2USDFMNuLSklnrOSZLHPtT1RaUuTTUdvln97l\nxp2QpviDAgMBAAECggEAI6jwySdaFKYo+WtbSY+kalSC678t+/tGbT3O/jMp4amj\n1rKzuA/q5lEUEqM71mVoHL7hWK2c3I9Ni8DUp7HbLwUiutamgtYwz97PrwqxlZeY\nP9ZZo/h1j5iQhdvQlLqrwjsBxglobDJxIuOTXNe6q8EMKa/aTpWOy3rlaM+aiYyW\nbm6DxAdbm/ANd06GXxoZxeZyHbXt9a6u1QSt+ZzPueFyl/qgsmKA5UJRs7vDfVSV\nNnaltv8CJgcWGmSKM+vqCx3s0ye6XMJAaWgk1yK4JHXGio+xjdTP8Drj30yBafOT\nrCsk3pkUAFHRkONuOlkE7o1aSoB8KBHGdM1L8SohBQKBgQC7CUQ8HwBGdMqIEDiX\nDawldP0DSBS1vCH1enSEkbAp4EdGjtxYT9TZqxhOCOtWQqWLn/kTpOoElAKrZ9tX\nLCmyYrbSfJPNHMkow44brCnI+NcgH8ynXwUNefDCITkF6izhOqIi3faXW8RRV+we\nAtEGOMGBNQ7OL6UsaapksS04hQKBgQC2moBUEPs7mu0O9CSNFI1tDJpYoGv5Xa2h\nBZmfcLov404kSNghkyxvinUx8+tT6nWtEFt3QDD4NL/oMpzSQp2PHNYdiWvIHRDR\nTrYCuWdKZblGxA++HoczeVgMrJ5euZKno+LcVsEui1hHVvP9l6wnBA0Gh4h/LD5l\nX4WKmEK/ZwKBgAzp9eEkztMOi1FbtVyQoBsx+ao5Vty5carOWq8wk/ZAOwufH42l\nbW/eBV6V9RHOpjl+wSbfEx1fztNg518ceICNmkvsOBRIcYc0AWLdv0DWFZxtNac0\nw1eL3Ni6jIJhCdo/PySjKLyIpIRtSqtDzITedFXkgPIxjfkEDrTHGvU1AoGACCk1\njQxJPiu5Zo6wx4FgpLwIdeeNi9KM/QJUEFUobRV6m5KJ3k5GkSGeBUKChPiYk8iE\nXfsHdUBeR1Fjwt3pTskaJK3MnF/4LXKYHd0NabzMIAaJMZUJs9o7fi2E0nT2wflI\nHVLfRWZC5sVTGEVcE05SCrWp+w0OKNexDzWo3gcCgYB0sPS7I7wFEcra+IHF0e1U\nmzclwZhkvkVHdWBPjcoxrOqDrRKvI36MX0b7WhB+gNO3JGsqOpxY+ms5yuaa+W7G\nwCDKpWa7koAA76/y/Qd9OP3CupTSBiYSKu1wwvzCeb7BaLPAPbotyQRLgvPlNo4I\nwhCM98l6u7Idt0C4agTdqA==\n-----END PRIVATE KEY-----\n","client_email":"firebase-adminsdk-fbsvc@kleos-8e95f.iam.gserviceaccount.com","client_id":"104916901120994879548","auth_uri":"https://accounts.google.com/o/oauth2/auth","token_uri":"https://oauth2.googleapis.com/token","auth_provider_x509_cert_url":"https://www.googleapis.com/oauth2/v1/certs","client_x509_cert_url":"https://www.googleapis.com/robot/v1/metadata/x509/firebase-adminsdk-fbsvc%40kleos-8e95f.iam.gserviceaccount.com","universe_domain":"googleapis.com"}'
FCM_PROJECT_ID=kleos-8e95f

# Сохраните (Ctrl+O, Enter, Ctrl+X)
```

## Проверка работы

После настройки создайте новость в админ-панели и проверьте логи:
```bash
pm2 logs kleos-api --lines 100 | grep -i "fcm\|push\|notification"
```

Должны появиться сообщения о успешной отправке уведомлений.

