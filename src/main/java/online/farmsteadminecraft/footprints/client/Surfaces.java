package online.farmsteadminecraft.footprints.client;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Which blocks take prints and how. Keyed by block id for now; tag- and resource-pack-driven
 * surfaces (so modded sand and snow work out of the box) come in M2.
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

    /** @return the surface, or {@code null} if the block keeps no prints */
    public static Surface of(BlockState state) {
        return BY_ID.get(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
    }
}
