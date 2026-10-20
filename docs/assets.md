# Иллюстрации ФинДруга

## Источники

Изображения предоставлены пользователем для доработки приложения 20 октября 2026 года. Использованы исходные файлы из указанных пользователем папок; авторство и дополнительные условия лицензирования не заявляются.

- [Фоны комнаты в Google Drive](https://drive.google.com/drive/folders/1tyoSTBNQUw1w5wzWoVBeCGA-VbavSiNE) — 9 PNG.
- [Предметы категории «Нужно» в Google Drive](https://drive.google.com/drive/folders/1H7evNBqMvM8KfyISpEVQMA8ZF6Cpvk9y) — 10 PNG.
- [Предметы категории «Хочу» в Google Drive](https://drive.google.com/drive/folders/1St_ds6jLo8Yr3TSGONDuxBP5HNrHNQmw) — 10 PNG.
- [Выбранные пользователем цели в Google Drive](https://drive.google.com/drive/folders/1J978efJCiHGzJKoCqXnOGPaoI3lUaCNm) — 4 PNG: палатка, конструктор, фотоаппарат и звёздный проектор.
- [Иллюстрации подсказок в Google Drive](https://drive.google.com/drive/folders/1uBAEZZyoSX_ETX3jGSeDq-Zy6jKWnr6N) — `fox_hint.png` и `bublle.svg`.
- Заставка `fox_growth_explanation.png` — подготовленная иллюстрация роста лисёнка из предоставленного пользователем документа «ТЗ_ФинДруг_доработки_с_иллюстрациями_v2.docx» (встроенный файл `image9.png`), без изменения изображения.

Файлы Google Drive получены через подключенный Google Drive с сохранением оригинальных байтов. Приложение содержит локальные ресурсы WebP в `app/src/main/res/drawable-nodpi/` и не требует доступа к Google Drive во время игры.

## Подготовка

Для предоставленных пользователем изображений выполнены только уменьшение разрешения с сохранением пропорций и преобразование в WebP средствами Pillow. Рисунок и содержимое этих изображений не редактировались. Для изображений предметов сохранён альфа-канал.

| Группа | Исходное разрешение | Разрешение ресурса | WebP quality |
| --- | --- | --- | --- |
| Фоны | 1224 × 2320 | 1080 × 2047 | 90 |
| «Нужно», «Хочу» | 800 × 800 | 512 × 512 | 92 |
| Цели | 800 × 800 | 640 × 640 | 92 |

## Фоны

| Исходный PNG | Ресурс Android | Содержание |
| --- | --- | --- |
| `BG.png` | `room_default` | Базовая комната |
| `BG_astronomer.png` | `room_astronomer` | Комната астронома |
| `BG_constructor.png` | `room_constructor` | Комната с конструктором |
| `BG_park.png` | `room_park` | Парк |
| `BG_pets.png` | `room_pets` | Комната с питомцами |
| `BG_photographer.png` | `room_photographer` | Комната фотографа |
| `BG_projector.png` | `room_projector` | Комната со звёздным проектором |
| `BG_scout.png` | `room_scout` | Комната путешественника |
| `BG_sport.png` | `room_sport` | Спортивная комната |

## Цели

| Исходный PNG | Ресурс Android | Содержание |
| --- | --- | --- |
| `tent.png` | `goal_tent` | Палатка; фон `room_scout` |
| `constructor.png` | `goal_construction_set` | Набор конструктора; фон `room_constructor` |
| `sky_projector.png` | `goal_star_projector` | Звёздный проектор; фон `room_projector` |
| `camera.png` | `goal_camera` | Фотоаппарат; фон `room_photographer` |

## Категория «Нужно»

| Исходный PNG | Ресурс Android | Предмет |
| --- | --- | --- |
| `apple.png` | `category_need_apple` | Яблоко |
| `backpack.png` | `category_need_backpack` | Рюкзак |
| `boots.png` | `category_need_boots` | Ботинки |
| `bread.png` | `category_need_bread` | Хлеб |
| `jacket.png` | `category_need_jacket` | Куртка |
| `notebook.png` | `category_need_notebook` | Тетрадь |
| `soup.png` | `category_need_soup` | Суп |
| `toothbrush.png` | `category_need_toothbrush` | Зубная щётка |
| `toothpaste.png` | `category_need_toothpaste` | Зубная паста |
| `umbrella.png` | `category_need_umbrella` | Зонт |

## Категория «Хочу»

| Исходный PNG | Ресурс Android | Предмет |
| --- | --- | --- |
| `comic_book.png` | `category_want_comic_book` | Комикс |
| `doll.png` | `category_want_doll` | Кукла |
| `game_console.png` | `category_want_game_console` | Игровая приставка |
| `headphones.png` | `category_want_headphones` | Наушники |
| `ice_cream.png` | `category_want_ice_cream` | Мороженое |
| `kite.png` | `category_want_kite` | Воздушный змей |
| `robot.png` | `category_want_robot` | Робот |
| `skateboard.png` | `category_want_skateboard` | Скейтборд |
| `sunglasses.png` | `category_want_sunglasses` | Солнцезащитные очки |
| `teddy_bear.png` | `category_want_teddy_bear` | Плюшевый медведь |

## Подсказки к заданиям

| Исходный файл | Ресурс Android | Подготовка |
| --- | --- | --- |
| `fox_hint.png` | `drawable-nodpi/fox_task_hint.webp` | Из 864 × 1225 в 451 × 640 с сохранением пропорций и альфа-канала, WebP quality 94 |
| `bublle.svg` | `drawable/task_hint_bubble.xml` | Точный исходный SVG path и заливка `#F3F1FB` перенесены в VectorDrawable с viewport 814 × 613 |

Лисёнок обращён вправо и предназначен для размещения слева от текста. Хвост облачка направлен в нижнюю левую сторону. Рекомендуемая область текста в координатах viewport облачка: `x = 64…750`, `y = 56…476`; относительные отступы — около 8% по горизонтали, 9% сверху и 22,4% снизу.

Четыре мягкие тени SVG используют `feGaussianBlur`. Android VectorDrawable не поддерживает SVG-фильтры, поэтому в ресурсе сохранены форма и цвет облачка без этих теней. Исходный SVG сохранён без изменений в рабочей папке `tmp/hints-v0131/`.
