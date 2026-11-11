# Footprints — карточки Modrinth и CurseForge

Всё, что вводится руками при создании проектов. Тексты описаний — в [modrinth.md](modrinth.md) и [curseforge.md](curseforge.md) (английский, Markdown). Версии потом загружает CI ([release.yml](../.github/workflows/release.yml)), руками — только первое создание проекта.

## Общее

| Поле | Значение |
| --- | --- |
| Название | `Footprints` |
| Slug | Modrinth `farmstead-footprints` (id `Fn5VQG7b`), CurseForge `farmstead-footprints` (id `1799135`). CI reads the ids from repo variables `MODRINTH_ID` / `CURSEFORGE_ID` |
| Краткое описание (≤ 150 знаков, для обеих площадок) | `Realistic footprints for players and 47 mobs: pressed into snow, sand and mud, lit by your shaders, fading with time. Client-side.` |
| Автор / организация | Farmstead |
| Лицензия | MIT (код). Ассеты — CC BY-NC 4.0, оговорено в LICENSE. На Modrinth: `MIT`, ссылка на LICENSE |
| Иконка | [`docs/media/icon-512.png`](../docs/media/icon-512.png) (512×512, Codex по [art/icon.prompt.txt](../art/icon.prompt.txt)); в самом моде — 256×256 |
| Баннер | [`docs/media/banner.webp`](../docs/media/banner.webp) 1280×480, 6 с, цикл (2,8 МБ) — в описании Modrinth и README; [`banner.gif`](../docs/media/banner.gif) 800×300 (4,3 МБ) — в описании CurseForge |
| Логотип | [`docs/media/logo.png`](../docs/media/logo.png) — прозрачный: блок, «FOOTPRINTS» пиксельным шрифтом, «by Farmstead» |
| Карточка для соцсетей | [`docs/media/social-card.png`](../docs/media/social-card.png) 1280×640 — в настройки GitHub-репозитория (Social preview) |

## Modrinth

| Поле | Значение |
| --- | --- |
| Тип | Mod |
| Categories (основная) | `decoration` |
| Environment | Client: **Required**, Server: **Unsupported** |
| Loaders | Fabric, Quilt, Forge, NeoForge |
| Game versions | 1.20.1, 1.21, 1.21.1, 26.3 |
| Issues | https://github.com/shtanko-michael/farmstead-footprints/issues |
| Source | https://github.com/shtanko-michael/farmstead-footprints |
| Wiki | https://github.com/shtanko-michael/farmstead-footprints#readme |
| Discord | — (если появится сервер Farmstead — добавить) |
| Зависимости (ставит CI на каждую версию) | **никаких обязательных**, даже Fabric API; Mod Menu — optional (`mOgUt4GM`), только для Fabric-файлов |

У Modrinth нет отдельной категории «визуальное»: клиентская красота (Particle Rain, Falling Leaves, Wakes) живёт в `decoration`, поэтому и мы там.

## CurseForge

| Поле | Значение |
| --- | --- |
| Класс | Mods (Minecraft) |
| Main category | **Cosmetic** |
| Дополнительные | — (Cosmetic точнее всего; «Mobs» и «Utility & QoL» не про нас) |
| Описание | Markdown — выбрать тип описания Markdown и вставить [curseforge.md](curseforge.md) |
| Environment у файлов | Client |
| Modloaders | Fabric, Quilt, Forge, NeoForge (по файлу) |
| Game versions | 1.20.1, 1.21, 1.21.1, 26.3 (по файлу) |
| Java | 17 (1.20.1), 21 (1.21.x), 25 (26.3) |
| Source / Issues / Wiki | те же ссылки на GitHub |
| Relations (ставит CI) | обязательных нет; Mod Menu (`308702`) — Optional, только для Fabric-файлов |

## Файлы и версии

Каждый релиз — 6 файлов, одна версия мода на все:

| Файл | Loaders | Game versions | Java |
| --- | --- | --- | --- |
| `farmstead-footprints-fabric-X.Y.Z+1.20.1.jar` | fabric, quilt | 1.20.1 | 17 |
| `farmstead-footprints-forge-X.Y.Z+1.20.1.jar` | forge | 1.20.1 | 17 |
| `farmstead-footprints-fabric-X.Y.Z+1.21.1.jar` | fabric, quilt | 1.21, 1.21.1 | 21 |
| `farmstead-footprints-neoforge-X.Y.Z+1.21.1.jar` | neoforge | 1.21, 1.21.1 | 21 |
| `farmstead-footprints-fabric-X.Y.Z+26.3.jar` | fabric, quilt | 26.3 | 25 |
| `farmstead-footprints-neoforge-X.Y.Z+26.3.jar` | neoforge | 26.3 | 25 |

Номер версии на площадках: `X.Y.Z+<mc>-<loader>` (например `0.1.0+1.21.1-neoforge`) — уникален на каждый файл, а проверка обновлений в моде сравнивает только `X.Y.Z`. Название версии: `Footprints X.Y.Z (NeoForge 1.21.1)`.

## Галерея

Файлы — [`docs/media/gallery/`](../docs/media/gallery), 1920×1080, с шейдерами (Complementary Unbound). На обеих площадках у картинки есть заголовок и описание; первую отметить **Featured** (на Modrinth она же — обложка карточки в поиске).

| Файл | Title | Description |
| --- | --- | --- |
| `01-snow-trail.jpg` | A trail toward the mountains | Walk through fresh snow at sunset and your footprints stay behind you. |
| `02-wolf-pack.jpg` | The pack passed here | Wolves leave paw prints in the snow: every mob has its own tracks. |
| `03-savanna.jpg` | Across the savanna | A camel and a player on red sand: two-toed pads and bare feet side by side. |
| `04-chicken.jpg` | Small feet, too | Even a chicken leaves tiny three-toed prints. |
| `05-polar-bear.jpg` | Big paws | A polar bear heads for the sunlit cliff, huge clawed prints behind it. |
| `06-gait-top-view.jpg` | Every gait is different | From above: a bear, a wolf and a camel. The hind paw steps into the front print, as in real tracks. |
| `07-settings.jpg` | Settings for everyone | Presets in one click, every value adjustable in game. |

GIF функций для описаний — [`docs/media/gifs/`](../docs/media/gifs) (520 px, 12 к/с, 1,3–3,2 МБ): `snow_hero`, `wolves`, `savanna`, `chicken`, `gait`.

## Видео

Ролик 32 с, 1920×1080, 60 к/с, музыка ElevenLabs: `C:/Users/admin/minecraft-api/video/out/footprints/footprints-trailer.mp4` (65 МБ, в git не кладём; монтаж — композиция `FootprintsTrailer` в `minecraft-api/video/src/mods/Footprints.tsx`). Залить на YouTube — шаг владельца. Ссылку вставить: на CurseForge — во вкладку Videos проекта; на Modrinth отдельного поля нет, поэтому строкой `▶ Watch the trailer` сразу под баннером в описании.

- **YouTube title:** `Footprints — realistic footprints for Minecraft (Fabric, NeoForge, Forge)`
- **Description:** `Footprints by Farmstead presses real tracks into the world: players and 47 mobs leave their own prints in snow, sand, mud and soil, lit by your shaders and fading with time. Client-side, works on any server. Minecraft 1.20.1, 1.21.1, 26.3 — Fabric, Quilt, NeoForge, Forge. Download: https://modrinth.com/mod/farmstead-footprints · https://www.curseforge.com/minecraft/mc-mods/farmstead-footprints · Source: https://github.com/shtanko-michael/farmstead-footprints`
- **Tags:** minecraft, minecraft mod, footprints, fabric, neoforge, forge, shaders, iris, sodium, immersive, farmstead

## Публикация через CI

1. Владелец создаёт GitHub-репозиторий `shtanko-michael/farmstead-footprints` и проекты на Modrinth и CurseForge (поля выше).
2. В репозитории: секреты `MODRINTH_TOKEN`, `CURSEFORGE_TOKEN`; переменные `MODRINTH_ID`, `CURSEFORGE_ID` (ID проектов). Без них релиз уходит только на GitHub.
3. Релиз: поднять `mod.version` в `stonecutter.properties.toml`, добавить `RELEASE_NOTES/vX.Y.Z.md` и строку в `CHANGELOG.md`, закоммитить, поставить тег `vX.Y.Z` и запушить тег. CI проверит, что тег совпадает с `mod.version`, соберёт шесть jar, создаст GitHub Release и загрузит файлы на обе площадки.

## Что ещё не готово к публикации

- [x] Иконка 512×512, логотип, анимированный баннер, карточка для соцсетей.
- [x] Галерея (7 кадров), GIF функций, ролик 32 с.
- [ ] Ролик залить на YouTube и вставить ссылку в описания (шаг владельца).
- [ ] Слаг CurseForge проверить при создании проекта.
- [ ] Проверка Forge 1.20.1 с Embeddium + Oculus (по стандарту ROADMAP) — пока не проверялось.
- [ ] Замер FPS до/после (Definition of Done).
