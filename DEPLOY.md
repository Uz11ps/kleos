# Инструкция по деплою изменений

## 1. Закоммитить и запушить изменения в Git

```bash
# Проверяем статус изменений
git status

# Добавляем все изменения
git add .

# Создаем коммит с описанием изменений
git commit -m "Добавлена поддержка аватарки пользователя в админке и приложении"

# Пушим изменения в репозиторий
git push origin main
# или
git push origin master
```

## 2. Подключиться к серверу и обновить код

```bash
# Подключиться к серверу по SSH
ssh root@ваш_сервер
# или
ssh ваш_пользователь@ваш_сервер

# Перейти в директорию проекта
cd /opt/kleos/kleos

# Получить последние изменения из Git
git pull origin main
# или
git pull origin master
```

## 3. Пересобрать и перезапустить бэкенд

```bash
# Перейти в директорию сервера
cd /opt/kleos/kleos/server

# Установить зависимости (если были добавлены новые)
npm install

# Собрать TypeScript код
npm run build

# Перезапустить PM2 процесс
pm2 restart kleos-api

# Или перезапустить все процессы
pm2 restart all

# Проверить статус процессов
pm2 status

# Посмотреть логи (если нужно)
pm2 logs kleos-api
```

## 4. Пересобрать Android приложение

### Вариант A: Через Android Studio

1. Откройте проект в Android Studio
2. Выберите **Build → Rebuild Project** (или нажмите `Ctrl+Shift+F9`)
3. После успешной сборки:
   - Для тестирования: **Run → Run 'app'** (или `Shift+F10`)
   - Для релиза: **Build → Generate Signed Bundle / APK**

### Вариант B: Через командную строку

```bash
# В корне проекта Android (C:\Users\1\AndroidStudioProjects\Kleos)

# Очистить предыдущую сборку
./gradlew clean

# Собрать Debug APK
./gradlew assembleDebug

# Или собрать Release APK (требует подписи)
./gradlew assembleRelease

# APK будет находиться в:
# app/build/outputs/apk/debug/app-debug.apk
# или
# app/build/outputs/apk/release/app-release.apk
```

## 5. Проверка работы сервера

```bash
# На сервере проверить, что сервер запущен
pm2 status

# Проверить логи на ошибки
pm2 logs kleos-api --lines 50

# Проверить доступность API
curl http://localhost:8080/health
# или с внешнего адреса
curl https://api.kleos-study.ru/health
```

## Быстрая команда для деплоя (все в одном)

Если у вас настроен SSH доступ, можно выполнить все команды одной строкой:

```bash
# С локального компьютера
git add . && git commit -m "Обновление: добавлена поддержка аватарки" && git push && ssh root@ваш_сервер "cd /opt/kleos/kleos && git pull && cd server && npm install && npm run build && pm2 restart kleos-api"
```

## Важные замечания

1. **Переменные окружения**: Убедитесь, что на сервере есть файл `.env` с правильными настройками
2. **База данных**: Изменения в моделях (например, добавление поля `avatarUrl`) применятся автоматически при первом сохранении
3. **Загруженные файлы**: Убедитесь, что директория `uploads/` существует и имеет права на запись:
   ```bash
   mkdir -p /opt/kleos/kleos/server/uploads/images
   chmod -R 755 /opt/kleos/kleos/server/uploads
   ```
4. **Nginx/Reverse Proxy**: Если используется Nginx, возможно потребуется перезапустить его:
   ```bash
   sudo systemctl restart nginx
   # или
   sudo service nginx restart
   ```

## Откат изменений (если что-то пошло не так)

```bash
# На сервере
cd /opt/kleos/kleos
git log  # Посмотреть историю коммитов
git reset --hard HEAD~1  # Откатить последний коммит
cd server
npm run build
pm2 restart kleos-api
```

