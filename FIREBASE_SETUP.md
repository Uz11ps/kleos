# 🔥 Настройка Firebase для нового package name

## ⚠️ Важно

Я временно обновил `google-services.json` с новым package name `com.kleosedu.app`, но для полной работы Firebase нужно:

### Правильное решение (рекомендуется):

1. **Зайдите в Firebase Console**: https://console.firebase.google.com/
2. **Выберите проект**: `kleos-8e95f`
3. **Перейдите в**: Project Settings → Your apps → Android apps
4. **Добавьте новое Android приложение** с package name: `com.kleosedu.app`
5. **Скачайте новый `google-services.json`** и замените текущий файл

### Альтернативное решение (если Firebase не критичен для разработки):

Если вам не нужен Firebase для локальной разработки, можно временно отключить Google Services plugin для debug сборки:

```kotlin
// В app/build.gradle.kts
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Временно закомментируйте для debug
    // id("com.google.gms.google-services")
}

// И добавьте условное применение:
if (project.hasProperty("enableFirebase")) {
    apply(plugin = "com.google.gms.google-services")
}
```

---

## 📝 Что было изменено

Я обновил `package_name` в `google-services.json` с `com.kleos.education` на `com.kleosedu.app`.

**Это временное решение!** Для полной работы Firebase (FCM, Analytics) нужно добавить новое приложение в Firebase Console.

---

## ✅ Проверка

После обновления файла попробуйте собрать проект снова:
```bash
./gradlew assembleDebug
```

Если ошибка исчезла, значит временное решение работает. Но для production нужно обновить Firebase Console.
