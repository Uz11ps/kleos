# Принудительная пересборка проекта

Если ошибка "handshake.fill" все еще появляется после исправления файла, выполните следующие шаги:

## 1. Полная очистка проекта

В Xcode:
1. **Product → Clean Build Folder** (`Cmd + Shift + K`)
2. Закройте Xcode полностью

В Terminal:
```bash
# Очистка DerivedData
rm -rf ~/Library/Developer/Xcode/DerivedData/*

# Очистка модулей (если используется SPM)
rm -rf ~/Library/Developer/Xcode/DerivedData/*/SourcePackages

# Очистка кэша Swift Package Manager
rm -rf ~/Library/Caches/org.swift.swiftpm
```

## 2. Удалите приложение с устройства

На iPhone:
1. Найдите приложение Kleos
2. Долгое нажатие → Удалить приложение
3. Подтвердите удаление

## 3. Пересоберите и установите заново

В Xcode:
1. Откройте проект заново
2. **Product → Build** (`Cmd + B`) - дождитесь завершения
3. **Product → Run** (`Cmd + R`) - установите на устройство заново

## 4. Если проблема сохраняется

Проверьте, что вы работаете с правильной версией файла:

1. В Xcode откройте `MainTabView.swift`
2. Найдите строку 170
3. Должно быть: `DrawerMenuItem(icon: "person.2.fill", title: "Partners", action: {`
4. Если там `handshake.fill`, значит файл не синхронизирован

## 5. Альтернативное решение

Если ничего не помогает, можно временно закомментировать проблемную строку:

```swift
// DrawerMenuItem(icon: "person.2.fill", title: "Partners", action: {
//     // Navigate to partners
//     isPresented = false
// })
```

Или использовать другую иконку:

```swift
DrawerMenuItem(icon: "link.circle.fill", title: "Partners", action: {
```

