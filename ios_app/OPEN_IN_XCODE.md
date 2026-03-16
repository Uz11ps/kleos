# 🚀 Как открыть проект в Xcode на Mac

## Быстрый способ (5 минут)

### Шаг 1: Откройте Xcode
- Если Xcode не установлен, скачайте из App Store
- Откройте Xcode

### Шаг 2: Создайте новый проект
1. В Xcode: **File → New → Project** (или `Cmd + Shift + N`)
2. Выберите **iOS** → **App** → **Next**
3. Заполните форму:
   - **Product Name:** `Kleos`
   - **Team:** Выберите вашу команду (или оставьте None)
   - **Organization Identifier:** `com.kleos`
   - **Bundle Identifier:** `com.kleosedu.app` (будет автоматически)
   - **Interface:** ⚠️ **SwiftUI** (важно!)
   - **Language:** ⚠️ **Swift** (важно!)
   - **Storage:** None
4. Нажмите **Next**
5. **Сохраните проект** в папку `ios_app` (рядом с папкой `Kleos`)

### Шаг 3: Скопируйте файлы
1. **Закройте Xcode** (если открыт)
2. Откройте **Finder**
3. Перейдите в папку `ios_app/Kleos/`
4. **Выделите все** файлы и папки:
   - `Models/`
   - `Network/`
   - `Utils/`
   - `Views/`
   - `KleosApp.swift`
5. **Скопируйте** (`Cmd + C`)
6. Перейдите в папку вашего проекта Xcode (где находится `ContentView.swift`)
7. **Вставьте** (`Cmd + V`)

### Шаг 4: Добавьте файлы в Xcode проект
1. **Откройте проект** в Xcode (двойной клик на `Kleos.xcodeproj`)
2. В левой панели (Project Navigator) найдите папку `Kleos` (синяя иконка)
3. **Правой кнопкой** на папку `Kleos` → **"Add Files to Kleos..."**
4. Выберите все скопированные папки и файлы:
   - `Models/`
   - `Network/`
   - `Utils/`
   - `Views/`
   - `KleosApp.swift`
5. **Важно:** Убедитесь что:
   - ✅ "Create groups" **отмечено**
   - ✅ "Copy items if needed" **НЕ отмечено** (файлы уже скопированы)
   - ✅ Target "Kleos" **отмечен**
6. Нажмите **Add**

### Шаг 5: Удалите старый ContentView.swift
1. Найдите `ContentView.swift` в Project Navigator
2. **Правой кнопкой** → **Delete** → **Move to Trash**

### Шаг 6: Проверьте главный файл
1. Откройте `KleosApp.swift`
2. Убедитесь, что в начале файла есть `@main`:
   ```swift
   @main
   struct KleosApp: App {
       ...
   }
   ```

### Шаг 7: Запустите проект! 🎉
1. Выберите **симулятор** в верхней панели (например, "iPhone 15 Pro")
2. Нажмите **▶️ Run** (или `Cmd + R`)
3. Приложение должно запуститься!

---

## Альтернативный способ через Terminal

Если вы предпочитаете командную строку:

```bash
# Перейдите в папку проекта
cd /Users/uz1ps/Downloads/kleos-main/ios_app

# Откройте Xcode (проект создастся вручную)
open -a Xcode
```

Затем выполните шаги 2-7 выше.

---

## Возможные проблемы

### ❌ "Cannot find type 'X' in scope"
**Решение:** Убедитесь, что все файлы добавлены в Target:
1. Выберите файл в Project Navigator
2. В правой панели (File Inspector) проверьте **Target Membership**
3. Убедитесь, что "Kleos" отмечен

### ❌ "Multiple commands produce..."
**Решение:** Удалите дубликаты файлов из проекта

### ❌ Приложение не запускается
**Решение:**
- Проверьте, что `KleosApp.swift` имеет `@main`
- Убедитесь, что `SplashView` существует

---

## Готово! ✅

Теперь проект должен открываться и запускаться в Xcode!



