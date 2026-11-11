package online.farmsteadminecraft.footprints.client;

import java.util.HashMap;
import java.util.Map;


/**
 * Hand-tuned vanilla surfaces by block id. The general rule for every other block (hardness gate,
 * include/exclude lists with tags, sound type) is in {@link SurfaceRules}.
 */
public final class Surfaces {
    private static final Map<String, Surface> BY_ID = new HashMap<>();

    static {
        put(Surface.SNOW, "snow", "snow_block", "powder_snow");
        put(Surface.SAND, "sand", "red_sand", "suspicious_sand");
        put(Surface.MUD, "mud", "muddy_mangrove_roots");
        put(Surface.GRAVEL, "gravel", "suspicious_gravel");
        put(Surface.SOIL, "dirt", "coarse_dirt", "rooted_dirt", "podzol", "mycelium", "farmland");
        put(Surface.CLAY, "clay");
        put(Surface.SOUL, "soul_sand", "soul_soil");
    }

    private static void put(Surface surface, String... ids) {
        for (String id : ids) BY_ID.put("minecraft:" + id, surface);
    }

    private Surfaces() {
    }

    /** The hand-tuned vanilla surfaces; everything else goes through {@link SurfaceRules}. */
    static Surface byId(String id) {
        return BY_ID.get(id);
    }
}
