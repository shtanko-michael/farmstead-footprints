package online.farmsteadminecraft.footprints.client;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import online.farmsteadminecraft.footprints.Footprints;
import online.farmsteadminecraft.footprints.ModLoaderAccess;
import online.farmsteadminecraft.footprints.config.BoolValue;
import online.farmsteadminecraft.footprints.config.ConfigFile;
import online.farmsteadminecraft.footprints.config.DoubleValue;
import online.farmsteadminecraft.footprints.config.IntValue;
import online.farmsteadminecraft.footprints.config.StringListValue;

/** {@code config/farmstead_footprints-client.toml}: every client setting, its comment, range and presets. */
public final class FootprintsConfig {
    public static final ConfigFile FILE = new ConfigFile(Footprints.MOD_ID, "client", 1);

    public static final BoolValue ENABLED = FILE.bool("general", "enabled", true)
        .comment("Master switch. Off: no prints are made or drawn.");
    public static final BoolValue CHECK_UPDATES = FILE.bool("general", "checkUpdates", true)
        .comment("Once at start-up, ask Modrinth whether a newer Footprints is out and say so in the log. Sends nothing about you.");

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
    public static final StringListValue ENTITY_BLACKLIST = FILE.list("footprints", "entityBlacklist", List.of())
        .comment("Entity ids that never leave prints, e.g. [\"minecraft:villager\"].");

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
        return problems;
    }

    public static void save() {
        FILE.save(dir(), new java.util.ArrayList<>());
        FootprintsClient.POOL.resize(MAX_PRINTS.value());
    }
}
