package online.farmsteadminecraft.footprints.client;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Picks a {@link FootProfile} per entity type by registry id, so it works the same on every
 * Minecraft version (class names of mobs move between versions, ids do not).
 * Unknown and flying entities leave no prints. Becomes data-driven in M2.
 */
public final class FootProfiles {
    private static final Map<String, FootProfile> BY_ID = new HashMap<>();

    static {
        for (String id : new String[]{"villager", "wandering_trader", "witch", "pillager", "vindicator",
                "evoker", "illusioner", "piglin", "piglin_brute"}) {
            BY_ID.put("minecraft:" + id, FootProfile.BOOTS);
        }
        for (String id : new String[]{"zombie", "husk", "drowned", "zombie_villager", "zombified_piglin",
                "skeleton", "stray", "wither_skeleton", "bogged", "enderman"}) {
            BY_ID.put("minecraft:" + id, FootProfile.BAREFOOT);
        }
        put(FootProfile.WOLF, "wolf");
        put(FootProfile.FOX, "fox");
        put(FootProfile.ARMADILLO, "armadillo");
        put(FootProfile.CAT, "cat");
        put(FootProfile.OCELOT, "ocelot");
        put(FootProfile.CREEPER, "creeper");
        put(FootProfile.HORSE, "horse", "skeleton_horse", "zombie_horse");
        put(FootProfile.DONKEY, "donkey");
        put(FootProfile.MULE, "mule");
        put(FootProfile.COW, "cow", "mooshroom");
        put(FootProfile.LLAMA, "llama", "trader_llama");
        put(FootProfile.HOGLIN, "hoglin", "zoglin");
        put(FootProfile.PIG, "pig", "sheep");
        put(FootProfile.GOAT, "goat");
        put(FootProfile.BIRD, "chicken");
        put(FootProfile.GIANT, "iron_golem", "warden");
        put(FootProfile.RAVAGER, "ravager");
        put(FootProfile.POLAR_BEAR, "polar_bear");
        put(FootProfile.PANDA, "panda");
        put(FootProfile.SNIFFER, "sniffer");
        put(FootProfile.CAMEL, "camel");
    }

    private FootProfiles() {
    }

    private static void put(FootProfile profile, String... ids) {
        for (String id : ids) BY_ID.put("minecraft:" + id, profile);
    }

    /** @return the profile, or {@code null} if this entity leaves no prints */
    public static FootProfile of(LivingEntity entity) {
        if (entity instanceof Player player) {
            if (!FootprintsConfig.PLAYERS.on()) return null;
            return player.getItemBySlot(EquipmentSlot.FEET).isEmpty() ? FootProfile.BAREFOOT : FootProfile.BOOTS;
        }
        if (!FootprintsConfig.MOBS.on()) return null;
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        if (FootprintsConfig.ENTITY_BLACKLIST.contains(id)) return null;
        FootProfile profile = BY_ID.get(id);
        if (profile != null && entity.isBaby() && !profile.quadruped()) return FootProfile.CHILD;
        return profile;
    }
}
