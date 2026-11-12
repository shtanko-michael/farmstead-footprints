# Changelog

All notable changes to Footprints. Versions follow [Semantic Versioning](https://semver.org/).

## [0.1.2] — unreleased

- Update check: the log now always gets exactly one line about it: the latest version (and the catalogue that said so), a newer version with its download link, or that Modrinth and CurseForge could not be reached (with a short reason, no stack trace). Before, only a newer version was logged, so a check that failed or found nothing was invisible.
- Update check falls back to CurseForge when Modrinth cannot answer (unreachable, project not public, no versions for the loader and Minecraft version). The About tab shows which catalogue answered.
- With `general.checkUpdates = false` or `-Dfarmstead.updateCheck=false` the log says `update check off`.
- `/footprints stats` reported the last frame that had prints ("0 prints of 2048, drawn last frame: 142") after every print had faded; it now says 0.
- The config file header named a command that does not exist (`/farmstead_footprints reload`); it now says `/footprints reload`.
- Forge 1.20.1: no more "Reference map 'farmstead_footprints.refmap.json' ... could not be read" warning in every player's log (the jar registered an empty mixin config pointing at a refmap that is never built); the Fabric mixin config is no longer packed into the Forge jar.

## [0.1.1] — 2026-11-12

- Settings screen on Minecraft 26.x: the title, the setting names and the About tab are centred again (26.x draws text widgets left-aligned).

## [0.1.0] — 2026-11-12

First release.

- Footprints for players and 47 mobs, each with its own foot shape; children and baby zombies leave smaller prints.
- Four-legged animals leave real track patterns: one zig-zag line as wide as the animal's stance (hip positions from each vanilla model), the hind foot landing in the front print of the same side; bears overstep slightly and walk pigeon-toed; sniffers put all three feet of a side in one print. Steps lengthen with speed.
- Soft surfaces: snow, powder snow, sand, red sand, mud, muddy mangrove roots, gravel, dirt, coarse dirt, rooted dirt, podzol, mycelium, farmland, clay, soul sand, soul soil. Prints sit on top of thin snow layers.
- Two-layer prints (hollow and lip) coloured from the ground they sit on; they fade after 1.5–5 minutes depending on the surface.
- Works with Sodium and Iris shader packs.
- Settings: presets (`default`, `vanilla_plus`, `cinematic`, `performance`), in-game settings screen (Mod Menu on Fabric, mod list on NeoForge/Forge), commented config `farmstead_footprints-client.toml`, commands `/footprints config|reload|stats`.
- 12 languages.
- Farmstead start-up line in the log and a once-per-session update check against Modrinth (off with `general.checkUpdates` or `-Dfarmstead.updateCheck=false`).
- Two looks: detailed (hollow and lip) and pixel (the block's own texture pixels pressed in).
- Prints crumble from the edges as they age; rain wears exposed prints; prints go with a broken block.
- Modded mobs get feet from their model; any soft modded block takes prints (hardness gate, id/#tag lists).
- Water: boat wakes, wading rings, swim bubbles, damp prints on hard ground, droplets; minecart sparks.
- Own prints from PNG files, random variants, resource-pack shapes, base textures and rules; F3+T reload.
- Settings screen with tabs, block/mob pickers, an About tab and "Try it".
- No dependencies on any loader, not even Fabric API.
- Minecraft 1.20.1 (Fabric, Forge); 1.21–1.21.5, 1.21.7–1.21.11, 26.1–26.3 (Fabric, NeoForge).
