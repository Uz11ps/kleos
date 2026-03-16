# Инструкция: Как клонировать iOS проект на Mac

## Шаг 1: Клонируйте репозиторий

Откройте **Terminal** на Mac и выполните:

```bash
git clone https://github.com/Uz11ps/kleos.git
cd kleos
```

## Шаг 2: Перейдите в папку iOS проекта

```bash
cd ios_app
```

## Шаг 3: Создайте новый проект в Xcode

1. **Откройте Xcode** (если не установлен, скачайте из App Store)

2. **Создайте новый проект:**
   - File → New → Project
   - Выберите **iOS** → **App**
   - Нажмите **Next**

3. **Настройте проект:**
   - **Product Name:** `Kleos`
   - **Team:** Выберите вашу команду (или оставьте None)
   - **Organization Identifier:** `com.kleos`
   - **Bundle Identifier:** `com.kleosedu.app` (будет автоматически)
   - **Interface:** **SwiftUI** ⚠️ (важно!)
   - **Language:** **Swift** ⚠️ (важно!)
   - **Storage:** None (или Core Data, если нужно)
   - Нажмите **Next**

4. **Выберите место для сохранения:**
   - Создайте новую папку, например `~/Desktop/Kleos-iOS`
   - Нажмите **Create**

## Шаг 4: Скопируйте файлы из репозитория

После создания проекта в Xcode:

1. **Закройте Xcode** (если открыт)

2. **Скопируйте все файлы** из `ios_app/Kleos/` в ваш новый проект:

```bash
# Перейдите в папку с клонированным репозиторием
cd ~/путь/к/kleos/ios_app/Kleos

# Скопируйте все файлы в ваш новый проект Xcode
cp -R Models Network Utils Views KleosApp.swift ~/Desktop/Kleos-iOS/Kleos/
```

**Или вручную через Finder:**
- Откройте папку `kleos/ios_app/Kleos/` в Finder
- Скопируйте все папки и файлы:
  - `Models/`
  - `Network/`
  - `Utils/`
  - `Views/`
  - `KleosApp.swift`
- Вставьте их в папку вашего проекта Xcode (где находится `ContentView.swift`)

## Шаг 5: Добавьте файлы в Xcode проект

1. **Откройте проект в Xcode**

2. **В левой панели (Project Navigator)** нажмите правой кнопкой на папку `Kleos` (синяя иконка)

3. **Выберите:** "Add Files to Kleos..."

4. **Выберите все скопированные папки и файлы:**
   - `Models/`
   - `Network/`
   - `Utils/`
   - `Views/`
   - `KleosApp.swift`

5. **Убедитесь, что:**
   - ✅ "Copy items if needed" **НЕ отмечено** (файлы уже скопированы)
   - ✅ "Create groups" **отмечено**
   - ✅ Target "Kleos" **отмечен**

6. **Нажмите Add**

## Шаг 6: Настройте главный файл приложения

1. **Удалите старый `ContentView.swift`** (если он есть):
   - Найдите его в Project Navigator
   - Правой кнопкой → Delete → Move to Trash

2. **Убедитесь, что `KleosApp.swift` является главным файлом:**
   - Откройте `KleosApp.swift`
   - Убедитесь, что в начале файла есть `@main`:
   ```swift
   @main
   struct KleosApp: App {
       ...
   }
   ```

3. **Если `ContentView.swift` остался и используется:**
   - Откройте `KleosApp.swift`
   - Замените `ContentView()` на `SplashView()` в `body`

## Шаг 7: Настройте API URL

1. **Откройте** `Network/ApiClient.swift`

2. **Проверьте** `baseURL`:
   ```swift
   let baseURL = "https://api.kleos-study.ru"
   ```

3. **Если нужно изменить** - измените на ваш URL

## Шаг 8: Запустите проект

1. **Выберите симулятор** или подключите iPhone:
   - В верхней панели Xcode выберите симулятор (например, "iPhone 15 Pro")

2. **Нажмите** ▶️ (Run) или `Cmd + R`

3. **Приложение должно запуститься!** 🎉

## Возможные проблемы и решения

### Ошибка: "Cannot find type 'X' in scope"
- **Решение:** Убедитесь, что все файлы добавлены в Target
  - Выберите файл в Project Navigator
  - В правой панели (File Inspector) проверьте Target Membership
  - Убедитесь, что "Kleos" отмечен

### Ошибка: "Multiple commands produce..."
- **Решение:** Удалите дубликаты файлов из проекта

### Ошибка компиляции с моделями
- **Решение:** Убедитесь, что все модели в `Models/Models.swift` правильно определены

### Приложение не запускается
- **Решение:** 
  - Проверьте, что `KleosApp.swift` имеет `@main`
  - Убедитесь, что `SplashView` существует и правильно импортирован

## Структура проекта в Xcode должна выглядеть так:

```
Kleos (проект)
├── Kleos (группа)
│   ├── KleosApp.swift
│   ├── Models/
│   │   └── Models.swift
│   ├── Network/
│   │   └── ApiClient.swift
│   ├── Utils/
│   │   ├── SessionManager.swift
│   │   └── UIUtils.swift
│   └── Views/
│       ├── SplashView.swift
│       ├── AuthView.swift
│       ├── MainTabView.swift
│       ├── HomeView.swift
│       ├── NewsView.swift
│       ├── UniversitiesView.swift
│       ├── ProgramsView.swift
│       ├── GalleryView.swift
│       ├── PartnersView.swift
│       ├── ProfileView.swift
│       ├── ChatView.swift
│       └── AdmissionView.swift
└── Assets.xcassets
```

## Готово! 🎉

Теперь у вас есть полная iOS-версия приложения, идентичная Android-версии!

Если возникнут вопросы - пишите!

