# Footprints

**Realistic footprints for players and mobs.** Walk through snow, sand or mud and your steps stay behind you, pressed into the ground, lit like the blocks around them, fading over time. Wolves leave paw prints, horses leave hooves, chickens leave three-toed scratches. Client-side: install it and it works on any server, vanilla included.

[![Modrinth](https://img.shields.io/modrinth/dt/footprints?logo=modrinth&label=Modrinth)](https://modrinth.com/mod/footprints)
[![CurseForge](https://img.shields.io/badge/CurseForge-Footprints-f16436?logo=curseforge)](https://www.curseforge.com/minecraft/mc-mods/footprints)
[![License: MIT](https://img.shields.io/badge/code-MIT-blue)](LICENSE)

By [Farmstead](https://farmsteadminecraft.online). Русская версия — [README.ru.md](README.ru.md).

## Features

- **Prints that look pressed in, not painted on.** Each print has a hollow and a raised lip, coloured from the ground it sits on: blue shadows in snow, darker sand in sand, a wet sheen in dark mud.
- **Players and 47 mobs**, each with its own foot: boots or bare feet, paws, cat paws, hooves, cloven hooves, bird feet, bear paws, camel feet, the giant feet of golems, ravagers and wardens. Children and baby zombies leave smaller prints.
- **A real gait.** Two-legged walkers alternate left and right; four-legged animals put the hind foot into the print of the front foot on the same side, the way real tracks look.
- **Surfaces:** snow and powder snow, sand and red sand, mud and muddy mangrove roots, gravel, dirt, coarse dirt, rooted dirt, podzol, mycelium, farmland, clay, soul sand and soul soil. Thin snow layers count: the print sits on top of the snow, not under it.
- **Prints fade.** Snow keeps them about five minutes, mud four, sand three, gravel a minute and a half; one setting stretches or shortens them all.
- **Shaders welcome.** Works with Sodium and Iris (tested with Complementary Unbound).
- **Settings for everyone:** presets in one line (`default`, `vanilla_plus`, `cinematic`, `performance`), an in-game settings screen (Mod Menu on Fabric, the mod list on NeoForge and Forge), a commented config file, and `/footprints reload`.
- **12 languages:** English, Russian, Chinese (Simplified), Portuguese (Brazil), Spanish, German, French, Polish, Ukrainian, Japanese, Korean, Turkish.

## Downloads and versions

| Minecraft | Fabric / Quilt | NeoForge | Forge | Java |
| --- | --- | --- | --- | --- |
| 26.3 | yes | yes | — | 25 |
| 1.21 – 1.21.1 | yes | yes | — | 21 |
| 1.20.1 | yes | — | yes | 17 |

Get it from [Modrinth](https://modrinth.com/mod/footprints), [CurseForge](https://www.curseforge.com/minecraft/mc-mods/footprints) or [GitHub Releases](https://github.com/shtanko-michael/farmstead-footprints/releases).

**Requirements:** on Fabric, [Fabric API](https://modrinth.com/mod/fabric-api). [Mod Menu](https://modrinth.com/mod/modmenu) is optional and adds the settings button. Nothing else.

**Server or client?** Client only. The server does not need it, and you can join any server with it. Installed on a dedicated server it only writes its start-up line to the log and checks for updates.

## Settings

Open them from Mod Menu (Fabric) or the mod list (NeoForge, Forge), type `/footprints config`, or edit `config/farmstead_footprints-client.toml`.

| Key | Default | Range | What it does |
| --- | --- | --- | --- |
| `preset` | `default` | `default`, `vanilla_plus`, `cinematic`, `performance` | Sets every value below at once |
| `general.enabled` | `true` | | Master switch |
| `general.checkUpdates` | `true` | | Look for a newer version on Modrinth once at start-up (see below) |
| `footprints.players` | `true` | | Prints of players, yourself included |
| `footprints.mobs` | `true` | | Prints of animals and monsters |
| `footprints.maxPrints` | `2048` | 64 – 16384 | Prints alive at once; the oldest go first |
| `footprints.renderDistance` | `48` | 8 – 128 | Blocks from the camera where prints are made and drawn |
| `footprints.lifetimeMultiplier` | `1.0` | 0.1 – 10.0 | How long prints stay |
| `footprints.opacity` | `0.95` | 0.1 – 1.0 | How strongly prints show |
| `footprints.entityBlacklist` | `[]` | entity ids | Entities that never leave prints, e.g. `["minecraft:villager"]` |

Presets: **vanilla_plus** — no mob prints, half the lifetime, lighter; **cinematic** — 8192 prints, 96 blocks, three times longer; **performance** — 512 prints, 24 blocks, half the lifetime.

In the file, keys that follow the preset are written commented out (`#maxPrints = 2048`). Remove the `#` to set a value yourself; your value then wins over the preset. A wrong value is reported in the log, and in chat after `/footprints reload`, and the preset value is used instead: the game never crashes over the config.

## Commands

Client-side commands, available to every player:

| Command | What it does |
| --- | --- |
| `/footprints config` | Opens the settings screen |
| `/footprints reload` | Re-reads the config file and reports problems |
| `/footprints stats` | Prints alive, the limit, and how many were drawn in the last frame |

The mod is client-side, so it has no server permissions. LuckPerms nodes come with the planned server module (trails worn into grass).

## Update check and privacy

When the game starts, Footprints writes one line to the log:

```
[Farmstead] Footprints 0.1.0 started (neoforge, Minecraft 1.21.1) - https://modrinth.com/mod/footprints
```

Then, once per session and in the background, it asks the public Modrinth API whether a newer release exists for your loader and Minecraft version. If one does, the log says so with a download link. The request carries no information about you, your world or your server; nothing is downloaded or installed. Turn it off with `general.checkUpdates = false`, or for every Farmstead mod at once with the JVM flag `-Dfarmstead.updateCheck=false` or the environment variable `FARMSTEAD_UPDATE_CHECK=false`.

## Compatibility

- **Sodium, Iris**: supported. Prints are drawn after the world, so shader packs light them.
- **Other footprint mods** (FootprintParticle and similar): both will draw their prints; keep one.
- **Modded blocks and mobs**: vanilla blocks and mobs only in 0.1. Tags for modded surfaces and creatures are planned.

## FAQ

**Do other players see my prints?** Everyone who has the mod sees everyone's prints, players and mobs alike, computed on their own client. Players without the mod see nothing.

**Is it heavy?** Prints are drawn in one batch per frame and capped by `maxPrints`. On a weak machine pick the `performance` preset.

**Why no prints on grass?** Grass is not a soft surface in 0.1. Trails worn into grass by walking are planned.

## Planned

Trails worn into grass that stay (server module with LuckPerms permissions), wet prints after rain and water, modded surfaces and mobs through block and entity tags.

## Building from source

Gradle runs on **JDK 25** (Stonecutter 0.10); the Java 17 and 21 toolchains are downloaded automatically.

```
./gradlew build                          # all six targets -> versions/<target>/build/libs
./gradlew :1.21.1-neoforge:runClient     # dev client of one target
python -I tools/lang.py --check          # all 12 language files are complete
```

## License

Code: [MIT](LICENSE). Assets (textures, images, video): [CC BY-NC 4.0](https://creativecommons.org/licenses/by-nc/4.0/). Modpacks may include the mod freely.

Made by [Farmstead](https://farmsteadminecraft.online). Bugs and ideas: [GitHub Issues](https://github.com/shtanko-michael/farmstead-footprints/issues).
