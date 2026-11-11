![Footprints — a polar bear walks into the sunset, leaving a trail of paw prints](https://raw.githubusercontent.com/shtanko-michael/farmstead-footprints/main/docs/media/banner.webp)

# Your steps stay behind you

**Footprints** presses real tracks into the world. Walk through snow, sand or mud and every step is left behind you: a hollow with a raised lip, coloured by the ground it sits in, lit by the same sun and shaders as everything around it, slowly fading away.

And it is not only you. **47 mobs** leave their own tracks: wolves trot past in a carpet of paw prints, horses leave hooves, camels two-toed pads, polar bears huge clawed paws, chickens tiny three-toed scratches.

**Client-side.** Install it and it works on any server, vanilla included. No server mod, no resource pack.

## What makes it different

- **Pressed in, not painted on.** Each print is two layers, a shadowed hollow and a lit lip, so the block texture shows through. Snow gets cold blue shadows, sand darkens, dark mud takes a wet sheen.
- **A real gait.** People step left, right, left. Four-legged animals put the hind foot into the front print of the same side, exactly like real animal tracks.
- **Every foot is its own.** Boots or bare feet, paws, cat paws, hooves, cloven hooves, bird feet, bear paws, camel feet, the giant feet of iron golems, ravagers and wardens. Children and baby zombies leave small prints.
- **Snow that behaves.** Prints sit on top of thin snow layers instead of disappearing under them.
- **Prints fade.** Snow keeps them about five minutes, mud four, sand three, gravel a minute and a half.
- **Shaders welcome.** Works with **Sodium** and **Iris**; shader packs light the prints like the ground.

## Surfaces

Snow, powder snow, sand, red sand, mud, muddy mangrove roots, gravel, dirt, coarse dirt, rooted dirt, podzol, mycelium, farmland, clay, soul sand, soul soil.

## Settings for everyone

- **One-line presets:** `default`, `vanilla_plus` (subtle, players only), `cinematic` (8192 prints, long-lasting, far), `performance`.
- **In-game settings screen:** Mod Menu on Fabric, the mod list on NeoForge and Forge, or `/footprints config`.
- **A config file that explains itself:** every key has a comment and a range; a wrong value never crashes the game, it is reported and replaced.
- Toggle players and mobs, the number of prints, distance, lifetime, opacity, and a blacklist of entities.
- Commands: `/footprints config`, `/footprints reload`, `/footprints stats`.

## 12 languages

English, Русский, 简体中文, Português (Brasil), Español, Deutsch, Français, Polski, Українська, 日本語, 한국어, Türkçe.

## Versions

| Minecraft | Loaders |
| --- | --- |
| 26.3 | Fabric, Quilt, NeoForge |
| 1.21 – 1.21.1 | Fabric, Quilt, NeoForge |
| 1.20.1 | Fabric, Quilt, Forge |

On Fabric it needs **[Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)**. **[Mod Menu](https://www.curseforge.com/minecraft/mc-mods/modmenu)** is optional.

## Coming next

Trails worn into grass that stay (a server module with LuckPerms permissions), wet prints after rain, modded surfaces and mobs through tags.

## FAQ

**Do other players see my prints?** Everyone who has the mod sees all prints, of players and mobs, computed on their own client.

**Is it heavy?** Prints are drawn in one batch per frame and capped by a limit you set. Weak PC? Pick the `performance` preset.

**Can I use it in a modpack?** Yes, freely.

**Does it phone home?** Once per start it asks the public Modrinth API whether a newer version exists and writes the answer to the log. Nothing about you is sent. Turn it off with `checkUpdates = false` or `-Dfarmstead.updateCheck=false`.

---

Made by **[Farmstead](https://farmsteadminecraft.online)** · [Source](https://github.com/shtanko-michael/farmstead-footprints) · [Issues](https://github.com/shtanko-michael/farmstead-footprints/issues) · [Documentation](https://github.com/shtanko-michael/farmstead-footprints#readme) · Code MIT, assets CC BY-NC 4.0
