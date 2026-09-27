# Где менять код

| Задача | Файл или папка |
| --- | --- |
| Добавить экран | `app/src/main/kotlin/ru/findrug/app/ui/screens/<раздел>/` |
| Подключить переход | `app/.../navigation/GameRoute.kt`, `GameNavigation.kt` |
| Добавить действие пользователя | `app/.../presentation/GameViewModel.kt` |
| Изменить правила и переходы игры | `core/domain/.../ScenarioGame.kt` |
| Изменить лимиты и награды периода | `core/domain/.../PeriodEconomy.kt` |
| Добавить ситуацию существующего типа | `core/domain/.../TaskCatalog.kt` |
| Добавить тип задания | `TaskDefinition.kt`, `TaskEvaluator.kt`, `app/.../tasks/TaskExercisePage.kt` |
| Изменить каталог товаров и целей | `core/domain/.../ScenarioContent.kt` |
| Изменить игровое состояние | `core/domain/.../ScenarioModels.kt` |
| Изменить формат сохранения | `app/.../data/ScenarioCodec.kt` и тесты сохранения |
| Изменить способ хранения | `app/.../data/GameRepository.kt`, `ScenarioStore.kt` |
| Общая кнопка или панель | `app/.../ui/components/GameComponents.kt` |
| Кнопки на экране, перелистывание | `app/.../ui/components/Page.kt` |
| Общее нижнее меню | `app/.../ui/components/GameBottomNavigation.kt`, подключено в `ui/GameRoot.kt` |
| Цвета и тема | `app/.../ui/theme/GameTheme.kt` |
| Ресурсы и значки | `app/.../ui/art/GameArt.kt`, `app/src/main/res/` |
| Вид и анимация лисёнка | `app/.../ui/pet/FoxScene.kt`, `core/domain/.../PetMotion.kt` |
| Звуки и жизненный цикл аудио | `app/.../audio/` |
| Серверный API | `backend/src/main/kotlin/ru/findrug/backend/` |

`...` в путях — соответствующий каталог Kotlin-пакета, например `app/src/main/kotlin/ru/findrug/app`.

Рабочие экраны находятся только в `app/ui/screens`. Каталогов `feature`, `legacy`, прежних `core/datastore`, `core/designsystem`, `core/model` больше нет. Серверные модели перенесены в backend. Архивы APK и исторические отчёты в `output/` и `docs/` не участвуют в сборке.

Новые экономические правила проверяются доменными тестами. Изменения сессии и сохранения — тестами ViewModel и хранилища. После изменения компоновки запускаются `PhoneLayoutTest` и сценарии интерфейса.

Прогноз срока цели: `core/domain/.../SavingsForecast.kt` и `app/.../savings/SavingsPage.kt`. Дополнительные вопросы о решении: `TaskCatalog.kt`, `TaskDefinition.kt`, `app/.../tasks/ReflectionPage.kt`.
