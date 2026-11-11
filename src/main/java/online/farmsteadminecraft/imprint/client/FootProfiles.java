package online.farmsteadminecraft.imprint.client;

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
        for (String id : new String[]{"wolf", "fox", "armadillo"}) {
            BY_ID.put("minecraft:" + id, FootProfile.PAW);
        }
        for (String id : new String[]{"cat", "ocelot", "creeper"}) {
            BY_ID.put("minecraft:" + id, FootProfile.SMALL_PAW);
        }
        for (String id : new String[]{"horse", "donkey", "mule", "skeleton_horse", "zombie_horse"}) {
            BY_ID.put("minecraft:" + id, FootProfile.HOOF);
        }
        for (String id : new String[]{"cow", "mooshroom", "llama", "trader_llama", "hoglin", "zoglin"}) {
            BY_ID.put("minecraft:" + id, FootProfile.CLOVEN);
        }
        for (String id : new String[]{"pig", "sheep", "goat"}) {
            BY_ID.put("minecraft:" + id, FootProfile.SMALL_HOOF);
        }
        BY_ID.put("minecraft:chicken", FootProfile.BIRD);
        for (String id : new String[]{"iron_golem", "ravager", "warden"}) {
            BY_ID.put("minecraft:" + id, FootProfile.GIANT);
        }
        for (String id : new String[]{"polar_bear", "panda", "sniffer", "camel"}) {
            BY_ID.put("minecraft:" + id, FootProfile.GIANT_PAW);
        }
    }

    private FootProfiles() {
    }

    /** @return the profile, or {@code null} if this entity leaves no prints */
    public static FootProfile of(LivingEntity entity) {
        if (entity instanceof Player player) {
            if (!ImprintConfig.players) return null;
            return player.getItemBySlot(EquipmentSlot.FEET).isEmpty() ? FootProfile.BAREFOOT : FootProfile.BOOTS;
        }
        if (!ImprintConfig.mobs) return null;
        FootProfile profile = BY_ID.get(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
        if (profile != null && entity.isBaby() && !profile.quadruped()) return FootProfile.CHILD;
        return profile;
    }
}
