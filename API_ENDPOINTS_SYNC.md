# ✅ Синхронизация API эндпоинтов iOS и Android

## Что было исправлено:

### 1. Базовый URL
- **iOS**: `https://api.kleos-study.ru/api`
- **Android**: `https://api.kleos-study.ru/api/`
- ✅ Оба используют один и тот же продакшн URL

### 2. Эндпоинты (все синхронизированы):

#### Auth
- ✅ `POST /auth/login`
- ✅ `POST /auth/register`
- ✅ `POST /auth/verify/consume`
- ✅ `POST /auth/verify/resend`

#### News
- ✅ `GET /news`
- ✅ `GET /news/{id}`

#### Users
- ✅ `GET /users/me`
- ✅ `PUT /users/me`
- ✅ `POST /users/fcm-token`

#### Universities
- ✅ `GET /universities`
- ✅ `GET /universities/{id}`

#### Programs
- ✅ `GET /programs?language=...&level=...&universityId=...&search=...`
- ✅ `GET /programs/{id}`

#### Gallery
- ✅ `GET /gallery`

#### Partners
- ✅ `GET /partners`

#### Admissions
- ✅ `POST /admissions`

#### Chats
- ✅ `POST /chats` - создание чата
- ✅ `GET /chats` - список чатов пользователя
- ✅ `GET /chats/{id}/messages` - получение сообщений
- ✅ `POST /chats/{id}/messages` - отправка сообщения

#### Settings
- ✅ `GET /settings/consent/{lang}`
- ✅ `GET /settings/countries`

## Структура Chat API (одинаковая в обоих приложениях):

1. **Создание/получение чата:**
   - iOS: `ensureChatId()` - автоматически создает или находит открытый чат
   - Android: `ChatsRepository.ensureChatId()` - то же самое

2. **Получение сообщений:**
   - iOS: `fetchMessages()` - использует `ensureChatId()` и получает `/chats/{id}/messages`
   - Android: `loadMessages()` - использует `ensureChatId()` и получает `/chats/{id}/messages`

3. **Отправка сообщения:**
   - iOS: `sendMessage(text:)` - использует `ensureChatId()` и отправляет в `/chats/{id}/messages`
   - Android: `sendMessage(text)` - использует `ensureChatId()` и отправляет в `/chats/{id}/messages`

## Результат:

✅ **iOS и Android теперь используют идентичные эндпоинты**
✅ **Оба используют продакшн URL: `https://api.kleos-study.ru/api/`**
✅ **Структура Chat API идентична в обоих приложениях**

## Проверка:

Проекты готовы к работе с реальным API сервером!


