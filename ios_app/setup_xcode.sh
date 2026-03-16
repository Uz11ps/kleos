#!/bin/bash

# Скрипт для автоматической настройки Xcode проекта для Kleos iOS

set -e

echo "🚀 Настройка Xcode проекта для Kleos iOS..."
echo ""

# Проверяем, что мы в правильной директории
if [ ! -d "Kleos" ]; then
    echo "❌ Ошибка: папка Kleos не найдена"
    echo "Запустите этот скрипт из папки ios_app"
    exit 1
fi

# Проверяем наличие Xcode
if ! command -v xcodebuild &> /dev/null; then
    echo "❌ Xcode не найден. Установите Xcode из App Store"
    exit 1
fi

PROJECT_NAME="Kleos"
BUNDLE_ID="com.kleosedu.app"

echo "📦 Создание Xcode проекта..."
echo "   Название: $PROJECT_NAME"
echo "   Bundle ID: $BUNDLE_ID"
echo ""

# Создаем временную директорию для проекта
TEMP_DIR=$(mktemp -d)
PROJECT_DIR="$TEMP_DIR/$PROJECT_NAME"

# Создаем структуру проекта
mkdir -p "$PROJECT_DIR/$PROJECT_NAME"

# Копируем все файлы из Kleos
echo "📋 Копирование файлов..."
cp -R Kleos/* "$PROJECT_DIR/$PROJECT_NAME/"

# Переходим в директорию проекта
cd "$PROJECT_DIR"

# Создаем проект через xcodebuild (это создаст базовую структуру)
echo "🔨 Создание проекта..."
xcodebuild -project "$PROJECT_NAME.xcodeproj" 2>/dev/null || true

# Если проект не создался автоматически, даем инструкции
if [ ! -f "$PROJECT_NAME.xcodeproj/project.pbxproj" ]; then
    echo ""
    echo "⚠️  Автоматическое создание проекта не удалось."
    echo "   Выполните следующие шаги вручную:"
    echo ""
    echo "1. Откройте Xcode"
    echo "2. File → New → Project"
    echo "3. Выберите iOS → App"
    echo "4. Настройте:"
    echo "   - Product Name: $PROJECT_NAME"
    echo "   - Organization Identifier: com.kleos"
    echo "   - Bundle Identifier: $BUNDLE_ID"
    echo "   - Interface: SwiftUI"
    echo "   - Language: Swift"
    echo "5. Сохраните проект в: $(pwd)"
    echo "6. После создания проекта, добавьте все файлы из папки $PROJECT_NAME"
    echo ""
    echo "Или используйте упрощенный способ ниже ⬇️"
    exit 0
fi

echo ""
echo "✅ Проект создан в: $PROJECT_DIR"
echo ""
echo "📝 Следующие шаги:"
echo "1. Откройте Xcode"
echo "2. File → Open → Выберите: $PROJECT_DIR/$PROJECT_NAME.xcodeproj"
echo "3. Убедитесь, что все файлы добавлены в Target"
echo "4. Выберите симулятор и нажмите Cmd+R для запуска"
echo ""



