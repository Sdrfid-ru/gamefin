# ФинДруг

Android-игра о финансовой грамотности для детей 6–12 лет. Ребёнок заботится о лисёнке,
планирует бюджет, выбирает между потребностями и желаниями и копит на мечту.

## Текущая версия

**0.11.0**, Android 8.0 и новее. Игра работает локально, без подключения к серверу.

- Профиль и настройка внешности лисёнка, анимация и звуки.
- Бюджет «Нужно / Хочу / Коплю», редактируемый до первого финансового действия.
- Шесть заданий, короткие тренировки и ограниченные награды за период.
- Восемь товаров, три базовые цели с улучшениями и коллекция полученных предметов.
- Итоги периода, сравнение плана и факта и рост питомца.
- Прогноз срока накопления и необязательные вопросы о причинах решений.
- Отдельный деморежим и локальное сохранение прогресса.

Актуальные правила: [экономика](docs/game-process-v10.md), [прогноз и вопросы](docs/learning-support-v11.md).
Результаты последней проверки функций: [отчёт 0.11](docs/emulator-audit-v11.md).
Отчёты предыдущих версий в `docs/` описывают состояние на момент проверки.

## Структура

| Папка | Назначение |
| --- | --- |
| `app/` | Android Compose, экраны, навигация, ViewModel, локальное хранение и звук |
| `core/domain/` | Правила, модели, контент и расчёты, независимые от Android |
| `backend/` | Отдельный Ktor-сервис; Android MVP к нему не подключён |
| `docs/` | Архитектура, правила, отчёты, скриншоты и лицензии ресурсов |
| `scripts/` | Генерация локальных звуковых ресурсов |

[Карта кода](docs/code-map.md) · [Архитектура](docs/architecture.md) · [Добавление контента](docs/content-model.md)

## Локальный запуск Android

Для запуска игры достаточно Android-приложения: сервер, Docker и файл `.env` не нужны.
Все команды ниже выполняются из корня проекта — папки с `settings.gradle.kts` и `gradlew`.

### Что установить

- **JDK 17** — требуется для модулей `core/domain` и `backend`.
- **Android Studio** с Android SDK и SDK Command-line Tools.
- В SDK Manager: **Android SDK Platform 37** (`platforms;android-37.0`),
  **Build-Tools 36.0.0** и **Platform-Tools**.
- Для запуска — эмулятор либо Android-устройство с Android 8.0 (API 26) или новее.

Проект использует `compileSdk = 37`, `targetSdk = 36`. Gradle устанавливается автоматически
через Wrapper из репозитория. Для первой сборки нужен интернет для загрузки зависимостей;
сама игра работает офлайн.

### Через Android Studio

1. Откройте корневую папку проекта и дождитесь Gradle Sync.
2. Укажите установленный JDK 17 в настройках Gradle JDK. Если используете встроенный JDK 21,
   дополнительно установите JDK 17 для Gradle toolchain.
3. В Device Manager создайте и запустите эмулятор. Для UI-тестов проверен Android 16 (API 36).
   Вместо эмулятора можно подключить телефон, включить отладку по USB и разрешить подключение.
4. Выберите модуль `app`, нужное устройство и нажмите **Run**.

### Через терминал Windows (PowerShell)

Укажите свои пути к JDK и SDK; `C:\path\to\jdk-17` ниже — пример, который нужно заменить:

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-17'
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"

.\gradlew.bat :app:assembleDebug
```

После запуска эмулятора или подключения телефона установите и откройте приложение:

```powershell
& "$env:ANDROID_HOME\platform-tools\adb.exe" devices
.\gradlew.bat :app:installDebug
& "$env:ANDROID_HOME\platform-tools\adb.exe" shell am start -n ru.findrug.app/.MainActivity
```

Устройство должно отображаться в списке `adb devices` со статусом `device`.
Для этих команд оставьте подключённым одно устройство или один эмулятор.

### Через терминал macOS / Linux (bash/zsh)

Укажите реальные пути к установленным JDK 17 и Android SDK:

```sh
export JAVA_HOME="/path/to/jdk-17"
export ANDROID_HOME="/path/to/Android/Sdk"
chmod +x gradlew
./gradlew :app:assembleDebug

# Запустите эмулятор или подключите телефон.
"$ANDROID_HOME/platform-tools/adb" devices
./gradlew :app:installDebug
"$ANDROID_HOME/platform-tools/adb" shell am start -n ru.findrug.app/.MainActivity
```

## Ресурсы

Происхождение изображений и шрифтов описано в [реестре ресурсов](docs/assets.md), сведения о звуках —
в [описании анимации и аудио](docs/pet-motion-audio-v5.md). Лицензии сторонних ресурсов находятся
в `docs/licenses/`.
