# Инструкция по правильной сборке APK

## Проблема: старый дизайн в APK

Если после изменений в APK всё ещё старый дизайн, это обычно связано с кэшированием. Выполните следующие шаги:

## Решение 1: Полная очистка и пересборка (Рекомендуется)

### В Android Studio:

1. **Сохраните все файлы**:
   - `File → Save All` (или `Ctrl+S`)

2. **Очистите проект**:
   - `Build → Clean Project`
   - Дождитесь завершения

3. **Очистите кэш Gradle**:
   - `File → Invalidate Caches / Restart...`
   - Выберите `Invalidate and Restart`
   - Дождитесь перезапуска Android Studio

4. **Пересоберите проект**:
   - `Build → Rebuild Project`
   - Дождитесь завершения сборки

5. **Соберите Release APK**:
   - `Build → Generate Signed Bundle / APK...`
   - Выберите `APK`
   - Выберите или создайте ключ подписи
   - Выберите `release` build variant
   - Нажмите `Finish`

### Через командную строку:

```bash
# Перейдите в корень проекта
cd C:\Users\1\AndroidStudioProjects\Kleos

# Очистите проект
gradlew clean

# Очистите кэш Gradle (опционально, но рекомендуется)
gradlew cleanBuildCache

# Удалите папку build (если проблема сохраняется)
# Windows:
rmdir /s /q app\build
rmdir /s /q build

# Пересоберите Release APK
gradlew assembleRelease

# APK будет находиться в:
# app\build\outputs\apk\release\app-release.apk
```

## Решение 2: Удаление кэша вручную

Если проблема сохраняется, удалите кэш вручную:

### Windows:

```bash
# Закройте Android Studio

# Удалите папки кэша
rmdir /s /q .gradle
rmdir /s /q app\build
rmdir /s /q build
rmdir /s /q .idea\caches

# Откройте Android Studio и выполните:
# File → Invalidate Caches / Restart → Invalidate and Restart
```

## Решение 3: Проверка версии кода

Убедитесь, что вы увеличили `versionCode` в `app/build.gradle.kts`:

```kotlin
defaultConfig {
    applicationId = "com.kleos.education"
    minSdk = 24
    targetSdk = 35
    versionCode = 2  // Увеличьте это число
    versionName = "1.1"  // Обновите версию
}
```

После изменения версии:
1. Синхронизируйте проект (`File → Sync Project with Gradle Files`)
2. Выполните `Clean Project`
3. Выполните `Rebuild Project`
4. Соберите новый APK

## Решение 4: Проверка что собирается правильный вариант

Убедитесь, что вы собираете **Release** вариант, а не Debug:

### В Android Studio:
- Внизу экрана выберите `Build Variants`
- Убедитесь, что выбран `release` для модуля `app`

### При сборке через меню:
- `Build → Generate Signed Bundle / APK...`
- Обязательно выберите `release` в списке `Build Variants`

## Решение 5: Проверка что изменения сохранены

Перед сборкой убедитесь:
1. Все изменённые файлы сохранены (нет красных точек в табах)
2. Проект синхронизирован (`File → Sync Project with Gradle Files`)
3. Нет ошибок компиляции (`Build → Make Project`)

## Быстрая команда для полной пересборки

```bash
# В корне проекта
gradlew clean cleanBuildCache assembleRelease --no-daemon
```

## Проверка APK

После сборки проверьте:
1. Размер APK должен быть примерно таким же или больше предыдущего
2. Дата создания файла должна быть текущей
3. Установите APK на устройство и проверьте изменения

## Если ничего не помогает

1. **Создайте новый проект** и скопируйте туда только исходный код (не папки build, .gradle)
2. Или **удалите весь кэш Gradle**:
   ```bash
   # Windows
   rmdir /s /q %USERPROFILE%\.gradle\caches
   ```

## Важные замечания

- **Всегда делайте Clean Project перед Release сборкой**
- **Используйте Release вариант для финальной сборки**
- **Увеличивайте versionCode при каждой новой версии**
- **Проверяйте что все файлы сохранены перед сборкой**


