package online.farmsteadminecraft.footprints.client;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import online.farmsteadminecraft.footprints.Footprints;
import online.farmsteadminecraft.footprints.ModLoaderAccess;
import online.farmsteadminecraft.footprints.config.BoolValue;
import online.farmsteadminecraft.footprints.config.ConfigFile;
import online.farmsteadminecraft.footprints.config.DoubleValue;
import online.farmsteadminecraft.footprints.config.EnumValue;
import online.farmsteadminecraft.footprints.config.IntValue;
import online.farmsteadminecraft.footprints.config.StringListValue;

/** {@code config/farmstead_footprints-client.toml}: every client setting, its comment, range and presets. */
public final class FootprintsConfig {
    public static final ConfigFile FILE = new ConfigFile(Footprints.MOD_ID, "client", 1).command("footprints");

    public static final BoolValue ENABLED = FILE.bool("general", "enabled", true)
        .comment("Master switch. Off: no prints are made or drawn.");
    public static final BoolValue CHECK_UPDATES = FILE.bool("general", "checkUpdates", true)
        .comment("Once at start-up, ask Modrinth (or CurseForge if Modrinth does not answer) whether a newer Footprints is out and say so in the log. Sends nothing about you.");

    public static final BoolValue PLAYERS = FILE.bool("footprints", "players", true)
        .comment("Prints of players, yourself included.");
    public static final BoolValue MOBS = FILE.bool("footprints", "mobs", true)
        .comment("Prints of animals and monsters.");
    public static final IntValue MAX_PRINTS = FILE.integer("footprints", "maxPrints", 2048, 64, 16384)
        .comment("How many prints can exist at once. The oldest disappear first when the limit is reached.");
    public static final IntValue RENDER_DISTANCE = FILE.integer("footprints", "renderDistance", 48, 8, 128)
        .comment("Blocks from the camera within which prints are made and drawn.");
    public static final DoubleValue LIFETIME = FILE.number("footprints", "lifetimeMultiplier", 1.0, 0.1, 10.0)
        .comment("Multiplies how long prints stay before they fade (snow 5 min, sand 3 min at 1.0).");
    public static final DoubleValue OPACITY = FILE.number("footprints", "opacity", 0.95, 0.1, 1.0)
        .comment("How strongly prints show.");
    public static final StringListValue ENTITY_BLACKLIST = FILE.list("footprints", "entityBlacklist", List.of(
            "minecraft:armor_stand", "minecraft:bat", "minecraft:bee", "minecraft:phantom", "minecraft:vex",
            "minecraft:allay", "minecraft:ghast", "minecraft:happy_ghast", "minecraft:blaze", "minecraft:breeze",
            "minecraft:wither", "minecraft:ender_dragon", "minecraft:slime", "minecraft:magma_cube",
            "minecraft:silverfish", "minecraft:endermite", "minecraft:snow_golem", "minecraft:shulker"))
        .comment("Entities that never leave prints, by id or #tag (flying and footless ones by default).");
    static {
        ENTITY_BLACKLIST.picker(online.farmsteadminecraft.footprints.config.StringListValue.Picker.ENTITIES);
    }
    public static final DoubleValue SIZE = FILE.number("footprints", "sizeMultiplier", 1.0, 0.25, 4.0)
        .comment("Makes every print bigger or smaller.");
    public static final DoubleValue LIFT = FILE.number("footprints", "lift", 0.0, -0.05, 0.5)
        .comment("Raises prints above the ground, in blocks. Raise it a little if a shader pack's deep ground hides them.");
    public static final EnumValue<Style> STYLE = FILE.choice("footprints", "style", Style.DETAILED)
        .comment("DETAILED: smooth prints with a hollow and a raised lip. PIXEL: prints pressed into the block's own texture "
            + "pixels, a dent drawn the way vanilla textures are drawn.");
    public static final EnumValue<Sneaking> SNEAKING = FILE.choice("footprints", "sneaking", Sneaking.SHALLOW)
        .comment("Sneaking feet leave shallow prints, or none at all (stealth).");
    public static final BoolValue INVISIBLE = FILE.bool("footprints", "invisible", false)
        .comment("Invisible players and mobs leave prints too.");
    public static final StringListValue PROFILES = FILE.list("footprints", "profiles", List.of())
        .comment("Prints per mob, overriding the built-in ones: \"<id or #tag> = shape:<name> size:<x> legs:<2|4|6|8> "
            + "stride:<blocks> stance:<blocks> length:<blocks>\", any part optional. Shapes: boot, bare, paw, cat, hoof, "
            + "cloven, bird, big, bear, or the file name of your own picture in config/farmstead_footprints/textures. "
            + "Example: \"minecraft:fox = shape:paw size:0.8\".");

    public static final BoolValue WET_PRINTS = FILE.bool("effects", "wetPrints", true)
        .comment("After water or rain, feet leave damp prints on hard ground too (stone, planks) that dry in half a minute.");
    public static final IntValue WET_SECONDS = FILE.integer("effects", "wetSeconds", 10, 1, 120)
        .comment("How long feet stay wet after leaving water or rain, in seconds.");
    public static final DoubleValue RAIN_WEAR = FILE.number("effects", "rainWear", 3.0, 1.0, 20.0)
        .comment("Prints under open sky age this many times faster while it rains or snows. 1: weather does nothing.");
    public static final EnumValue<Dust> DUST = FILE.choice("effects", "dust", Dust.ALL)
        .comment("A puff of snow, sand or dust from under the feet on loose ground.");
    public static final DoubleValue WET_OPACITY = FILE.number("effects", "wetOpacity", 1.0, 0.0, 2.0)
        .comment("How strongly damp prints on hard ground show, as a multiplier. 0 hides them.");
    public static final EnumValue<Dust> SPLASH = FILE.choice("effects", "splash", Dust.ALL)
        .comment("Droplets fly from wet feet for a few seconds after leaving water.");
    public static final BoolValue RIPPLES = FILE.bool("effects", "ripples", true)
        .comment("Rings spread on the water where players and mobs wade.");
    public static final BoolValue BOAT_WAKE = FILE.bool("effects", "boatWake", true)
        .comment("Boats leave a foam wake on the water and throw spray when fast.");
    public static final BoolValue SWIM_BUBBLES = FILE.bool("effects", "swimBubbles", true)
        .comment("Bubbles trail behind swimmers.");
    public static final BoolValue RAIL_SPARKS = FILE.bool("effects", "railSparks", true)
        .comment("Fast minecarts throw sparks from the rails, more on powered rails.");
    public static final BoolValue REMOVE_ON_CHANGE = FILE.bool("effects", "removeWhenBroken", true)
        .comment("A print goes when the block under it is broken or replaced.");
    public static final EnumValue<Decay> DECAY = FILE.choice("effects", "decay", Decay.CRUMBLE)
        .comment("How old prints go: CRUMBLE wears them from the edges in over the second half of their life, FADE just fades them out.");

    public static final DoubleValue HARDNESS = FILE.number("surfaces", "hardnessGate", 0.7, 0.0, 5.0)
        .comment("Any block softer than this takes prints, so modded soil and sand work out of the box "
            + "(sand 0.5, grass and gravel 0.6, wool 0.8). 0: only the blocks listed below.");
    public static final StringListValue SURFACE_INCLUDE = FILE.list("surfaces", "include", List.of("#minecraft:wool"))
        .comment("Blocks that always take prints, by id or #tag.");
    static {
        SURFACE_INCLUDE.picker(online.farmsteadminecraft.footprints.config.StringListValue.Picker.BLOCKS);
    }
    public static final StringListValue SURFACE_EXCLUDE = FILE.list("surfaces", "exclude", List.of(
            "#minecraft:leaves", "#minecraft:impermeable", "#minecraft:ice", "#minecraft:beds",
            "#minecraft:pressure_plates", "#minecraft:trapdoors", "#minecraft:beehives", "minecraft:scaffolding",
            "minecraft:slime_block", "minecraft:honey_block", "minecraft:tnt", "minecraft:target", "minecraft:composter",
            "minecraft:daylight_detector", "minecraft:glowstone", "minecraft:sea_lantern", "minecraft:redstone_lamp",
            "minecraft:magma_block", "minecraft:cactus", "minecraft:netherrack", "minecraft:big_dripleaf",
            "minecraft:turtle_egg", "minecraft:sniffer_egg", "minecraft:frogspawn"))
        .comment("Blocks that never take prints, by id or #tag; wins over the list above and the hardness rule.");
    static {
        SURFACE_EXCLUDE.picker(online.farmsteadminecraft.footprints.config.StringListValue.Picker.BLOCKS);
    }

    public enum Style { DETAILED, PIXEL }

    public enum Sneaking { SHALLOW, NONE }

    public enum Dust { OFF, PLAYERS, ALL }

    public enum Decay { CRUMBLE, FADE }

    static {
        FILE.preset("vanilla_plus", Map.of(
                "footprints.mobs", false,
                "footprints.lifetimeMultiplier", 0.5,
                "footprints.opacity", 0.75))
            .preset("cinematic", Map.of(
                "footprints.maxPrints", 8192,
                "footprints.renderDistance", 96,
                "footprints.lifetimeMultiplier", 3.0))
            .preset("performance", Map.of(
                "footprints.maxPrints", 512,
                "footprints.renderDistance", 24,
                "footprints.lifetimeMultiplier", 0.5));
    }

    private FootprintsConfig() {
    }

    public static Path dir() {
        return ModLoaderAccess.get().configDir();
    }

    /** Loads (or creates) the file and applies it; returns problems for the player. */
    public static List<String> load() {
        List<String> problems = FILE.load(dir());
        problems.forEach(p -> Footprints.LOGGER.warn("config: {}", p));
        FootprintsClient.POOL.resize(MAX_PRINTS.value());
        SurfaceRules.reset();
        FootProfiles.reset();
        FootprintAtlas.invalidate();
        return problems;
    }

    public static void save() {
        FILE.save(dir(), new java.util.ArrayList<>());
        FootprintsClient.POOL.resize(MAX_PRINTS.value());
        SurfaceRules.reset();
        FootProfiles.reset();
    }
}
