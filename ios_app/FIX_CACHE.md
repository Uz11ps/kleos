# Инструкция по очистке кэша Xcode

Если ошибка "No symbol named 'handshake.fill'" все еще появляется, выполните следующие шаги:

## 1. Очистка кэша Xcode

В Xcode:
1. **Product → Clean Build Folder** (или `Cmd + Shift + K`)
2. Закройте Xcode полностью
3. Откройте Terminal и выполните:

```bash
cd ~/Library/Developer/Xcode/DerivedData
rm -rf *
```

4. Перезапустите Xcode
5. Откройте проект заново
6. **Product → Build** (или `Cmd + B`)
7. **Product → Run** (или `Cmd + R`)

## 2. Если проблема сохраняется

Проверьте, что файл `MainTabView.swift` действительно содержит исправление:

```swift
DrawerMenuItem(icon: "person.2.fill", title: "Partners", action: {
```

Если там все еще `handshake.fill`, значит изменения не применились. В этом случае:
1. Убедитесь, что файл сохранен
2. Проверьте, что вы работаете с правильной веткой в Git
3. Перезагрузите файл в Xcode (закройте и откройте снова)

## 3. Альтернативное решение

Если проблема не решается, можно временно заменить иконку на другую:

```swift
DrawerMenuItem(icon: "building.2.fill", title: "Partners", action: {
```

Или:

```swift
DrawerMenuItem(icon: "link.circle.fill", title: "Partners", action: {
```

