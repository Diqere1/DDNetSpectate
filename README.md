# DDNetSpectate

Android-приложение для наблюдения за игрой DDNet (DDraceNetwork) в режиме реального времени.

## Скачать APK

### Автоматическая сборка через GitHub Actions

1. Перейдите во вкладку [**Actions**](https://github.com/Diqere1/DDNetSpectate/actions)
2. Выберите последний успешный workflow **Android CI Build** (с зеленой галочкой ✅)
3. Прокрутите вниз до секции **Artifacts**
4. Скачайте `DDNetSpectate-debug.zip`
5. Распакуйте архив и получите `app-debug.apk`
6. Установите APK на Android-устройство

### Ручной запуск сборки

1. Откройте вкладку [**Actions**](https://github.com/Diqere1/DDNetSpectate/actions)
2. Выберите **Android CI Build** в левом меню
3. Нажмите **Run workflow** → **Run workflow**
4. Дождитесь завершения (5-10 минут)
5. Скачайте APK из Artifacts

## Возможности

- Просмотр списка серверов DDNet с мастер-сервера
- Подключение к серверам в режиме наблюдателя
- Отображение карты через WebView
- Чат с игроками на сервере
- Визуализация позиций игроков в реальном времени

## Технологии

- **Frontend**: Java/Android (RecyclerView, WebView)
- **Backend**: Node.js (встроенный через JNI)
- **Связь**: Socket.IO (localhost:3000)
- **Библиотека**: teeworlds (npm) для подключения к DDNet серверам

## Требования

- Android 7.0+ (API Level 24)
- Поддерживаемые архитектуры: armeabi-v7a, arm64-v8a, x86, x86_64

## Лицензия

Forked from [Siraxa/DDNetSpectate](https://github.com/Siraxa/DDNetSpectate)
