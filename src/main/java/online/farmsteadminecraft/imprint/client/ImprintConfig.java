package online.farmsteadminecraft.imprint.client;

/**
 * Client settings. Defaults only for now; the TOML file, presets and the settings screen come in M2.
 */
public final class ImprintConfig {
    public static boolean enabled = true;
    public static boolean players = true;
    public static boolean mobs = true;
    public static int maxDecals = 2048;
    public static double renderDistance = 48.0;
    public static double lifetimeMultiplier = 1.0;
    public static float opacity = 0.95f;

    private ImprintConfig() {
    }
}
