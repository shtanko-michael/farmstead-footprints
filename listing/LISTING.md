# Footprints — карточки Modrinth и CurseForge

Всё, что вводится руками при создании проектов. Тексты описаний — в [modrinth.md](modrinth.md) и [curseforge.md](curseforge.md) (английский, Markdown). Версии потом загружает CI ([release.yml](../.github/workflows/release.yml)), руками — только первое создание проекта.

## Общее

| Поле | Значение |
| --- | --- |
| Название | `Footprints` |
| Slug | `footprints` (Modrinth: свободен на 11.11.2026). CurseForge: проверить при создании; если занят — `farmstead-footprints` и поправить ссылки (поиск по `mc-mods/footprints`) |
| Краткое описание (≤ 150 знаков, для обеих площадок) | `Realistic footprints for players and 47 mobs: pressed into snow, sand and mud, lit by your shaders, fading with time. Client-side.` |
| Автор / организация | Farmstead |
| Лицензия | MIT (код). Ассеты — CC BY-NC 4.0, оговорено в LICENSE. На Modrinth: `MIT`, ссылка на LICENSE |
| Иконка | 512×512 PNG — **ещё нет** (в моде сейчас 128×128 `assets/farmstead_footprints/icon.png`); сделать брендовую по стандарту ROADMAP |
| Баннер | анимированный WebP/GIF 1280×400–720 → `docs/media/banner.webp` — **ещё нет**, из финального ролика |

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
| Зависимости (ставит CI на каждую версию) | Fabric API — required (`P7dR8mSH`), Mod Menu — optional (`mOgUt4GM`); только для Fabric-файлов |

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
| Relations (ставит CI) | Fabric API (`306612`) — Required Dependency, Mod Menu (`308702`) — Optional, только для Fabric-файлов |

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

## Галерея (6–10 кадров 1920×1080, с подписями)

Снимаются тем же ригом, что и ролик (`media/film.py`), после одобрения раскадровки:

1. **A trail toward the mountains** — герой уходит к горе на закате, цепочка следов.
2. **The pack passed here** — три волка, ковёр отпечатков лап.
3. **Across the savanna** — верблюд и герой на красном песке.
4. **Small feet, too** — макро курицы с трёхпалыми следами.
5. **Big paws** — белый медведь, след к оранжевой скале.
6. **Every foot is different** — вид сверху: медведь, волк, верблюд рядом (`gait`).
7. **Settings** — экран настроек с пресетами.
8. **Works with shaders** — один кадр без шейдеров и с ними.

## Публикация через CI

1. Владелец создаёт GitHub-репозиторий `shtanko-michael/farmstead-footprints` и проекты на Modrinth и CurseForge (поля выше).
2. В репозитории: секреты `MODRINTH_TOKEN`, `CURSEFORGE_TOKEN`; переменные `MODRINTH_ID`, `CURSEFORGE_ID` (ID проектов). Без них релиз уходит только на GitHub.
3. Релиз: поднять `mod.version` в `stonecutter.properties.toml`, добавить `RELEASE_NOTES/vX.Y.Z.md` и строку в `CHANGELOG.md`, закоммитить, поставить тег `vX.Y.Z` и запушить тег. CI проверит, что тег совпадает с `mod.version`, соберёт шесть jar, создаст GitHub Release и загрузит файлы на обе площадки.

## Что ещё не готово к публикации

- [ ] Иконка 512×512 и анимированный баннер `docs/media/banner.webp`.
- [ ] Галерея и ролик (ждут одобрения раскадровки).
- [ ] Слаг CurseForge проверить при создании проекта.
- [ ] Проверка Forge 1.20.1 с Embeddium + Oculus (по стандарту ROADMAP) — пока не проверялось.
- [ ] Замер FPS до/после (Definition of Done).
