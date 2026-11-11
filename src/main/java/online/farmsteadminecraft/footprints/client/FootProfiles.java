package online.farmsteadminecraft.footprints.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import online.farmsteadminecraft.footprints.Footprints;

/**
 * Picks a {@link FootProfile} for any living entity, modded ones included:
 * <ol>
 *   <li>the exclude list ({@code footprints.entityBlacklist}, ids and {@code #tags}) wins;</li>
 *   <li>a built-in profile with the real gait of the vanilla model, or, for mobs we do not know, one
 *       made from the hitbox: tall and narrow walks on two feet, the rest on four;</li>
 *   <li>a {@code footprints.profiles} line for the id or a tag of the entity changes any part of it:
 *       shape (built-in or the player's own picture), size, number of legs, stride, stance, body length.</li>
 * </ol>
 * The result is cached per entity type and dropped on config reload, atlas rebuild and world change.
 * Keyed by registry id, so it works the same on every Minecraft version.
 */
public final class FootProfiles {
    private static final Map<String, FootProfile> BY_ID = new HashMap<>();
    private static final FootProfile NONE = FootProfile.biped(FootShape.SNEAK, FootShape.SNEAK, 1f, 0f, 0f);
    private static final Map<EntityType<?>, FootProfile> CACHE = new IdentityHashMap<>();
    private static FootProfile playerBoots, playerBare;
    private static List<Rule> rules;
    private static List<String> excludeIds;
    private static List<TagKey<EntityType<?>>> excludeTags;

    static {
        put(FootProfile.BOOTS, "villager", "wandering_trader", "witch", "pillager", "vindicator", "evoker",
            "illusioner", "piglin", "piglin_brute", "copper_golem");
        put(FootProfile.BAREFOOT, "zombie", "husk", "drowned", "zombie_villager", "zombified_piglin", "skeleton",
            "stray", "wither_skeleton", "bogged", "enderman", "creaking");
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
        put(FootProfile.PARROT, "parrot");
        put(FootProfile.GIANT, "iron_golem", "warden");
        put(FootProfile.RAVAGER, "ravager");
        put(FootProfile.POLAR_BEAR, "polar_bear");
        put(FootProfile.PANDA, "panda");
        put(FootProfile.SNIFFER, "sniffer");
        put(FootProfile.CAMEL, "camel", "camel_husk");
        put(FootProfile.RABBIT, "rabbit");
        put(FootProfile.FROG, "frog");
        put(FootProfile.SPIDER, "spider");
        put(FootProfile.CAVE_SPIDER, "cave_spider");
        put(FootProfile.TURTLE, "turtle");
        put(FootProfile.AXOLOTL, "axolotl");
    }

    private FootProfiles() {
    }

    private static void put(FootProfile profile, String... ids) {
        for (String id : ids) BY_ID.put("minecraft:" + id, profile);
    }

    public static void reset() {
        CACHE.clear();
        rules = null;
        playerBoots = playerBare = null;
    }

    /** @return the profile, or {@code null} if this entity leaves no prints */
    public static FootProfile of(LivingEntity entity) {
        if (entity.isInvisible() && !FootprintsConfig.INVISIBLE.on()) return null;
        if (rules == null) parse();
        if (entity instanceof Player player) {
            if (!FootprintsConfig.PLAYERS.on()) return null;
            if (playerBoots == null) {
                playerBoots = override(FootProfile.BOOTS, player.getType());
                playerBare = override(FootProfile.BAREFOOT, player.getType());
            }
            return player.getItemBySlot(EquipmentSlot.FEET).isEmpty() ? playerBare : playerBoots;
        }
        if (!FootprintsConfig.MOBS.on()) return null;
        FootProfile profile = CACHE.computeIfAbsent(entity.getType(), FootProfiles::resolve);
        if (profile == NONE) return null;
        if (entity.isBaby() && (profile == FootProfile.BOOTS || profile == FootProfile.BAREFOOT)) return FootProfile.CHILD;
        return profile;
    }

    private static FootProfile resolve(EntityType<?> type) {
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
        if (excludeIds.contains(id)) return NONE;
        for (TagKey<EntityType<?>> tag : excludeTags) {
            if (type.builtInRegistryHolder().is(tag)) return NONE;
        }
        FootProfile base = BY_ID.get(id);
        if (base == null) base = generic(type);
        return override(base, type);
    }

    /** Mobs we know nothing about: two feet if tall and narrow (humanoids, birds), four otherwise. */
    private static FootProfile generic(EntityType<?> type) {
        float w = Math.max(0.1f, type.getWidth()), h = Math.max(0.1f, type.getHeight());
        if (h > 1.6f * w) {
            float k = Math.min(2.5f, Math.max(0.3f, h / 1.95f));
            return FootProfile.biped(FootShape.BARE_LEFT, FootShape.BARE_RIGHT, 0.92f * k, 0.13f * Math.max(1f, w / 0.6f), k);
        }
        float stance = 0.3f * w, length = 1.2f * w;
        return FootProfile.WOLF.with(FootShape.PAW, Math.min(2.5f, Math.max(0.35f, w / 0.6f * 0.85f)), 0.7f * length,
            0f, FootProfile.fourLegs(stance, length));
    }

    private static FootProfile override(FootProfile base, EntityType<?> type) {
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
        Rule rule = null;
        // an exact id beats a tag; otherwise the first matching line wins
        for (Rule r : rules) {
            if (r.id != null) {
                if (r.id.equals(id)) {
                    rule = r;
                    break;
                }
            } else if (rule == null && type.builtInRegistryHolder().is(r.tag)) {
                rule = r;
            }
        }
        if (rule == null) return base;
        FootShape shape = rule.shape != null ? rule.shape : base.left();
        float scale = rule.size != null ? rule.size : base.scale();
        float stride = rule.stride != null ? rule.stride : base.stride();
        int legs = rule.legs != null ? rule.legs : base.quadruped() ? base.legs().length : 2;
        float stance = rule.stance != null ? rule.stance : base.quadruped() ? base.legs()[0] : base.spacing();
        if (legs <= 2) {
            return FootProfile.biped(shape, shape.right(), stride, stance, scale);
        }
        float[] hips = base.legs();
        boolean reshape = rule.stance != null || rule.length != null || hips == null || hips.length != legs;
        if (reshape) {
            float length = rule.length != null ? rule.length
                : hips != null ? hips[1] - hips[hips.length - 1] : 0.6f;
            int perSide = Math.max(2, legs / 2);
            hips = new float[perSide * 2];
            for (int i = 0; i < perSide; i++) {
                hips[i * 2] = stance;
                hips[i * 2 + 1] = length * (0.5f - i / (float) (perSide - 1));
            }
        }
        return base.with(shape, scale, stride, base.spacing(), hips);
    }

    private static void parse() {
        List<String> exIds = new ArrayList<>();
        List<TagKey<EntityType<?>>> exTags = new ArrayList<>();
        for (String entry : FootprintsConfig.ENTITY_BLACKLIST.get()) {
            String e = entry.trim();
            if (e.startsWith("#")) {
                ResourceLocation t = Footprints.parseId(e.substring(1));
                if (t != null) exTags.add(TagKey.create(Registries.ENTITY_TYPE, t));
            } else if (!e.isEmpty()) {
                ResourceLocation t = Footprints.parseId(e);
                if (t != null) exIds.add(t.toString());
            }
        }
        List<Rule> parsed = new ArrayList<>();
        // the player's lines first, then resource packs': the first matching line wins
        List<String> lines = new ArrayList<>(FootprintsConfig.PROFILES.get());
        lines.addAll(PackData.profiles());
        for (String line : lines) {
            Rule r = Rule.parse(line);
            if (r != null) parsed.add(r);
        }
        excludeIds = exIds;
        excludeTags = exTags;
        rules = parsed;
    }

    /** One {@code footprints.profiles} line: {@code <id or #tag> = shape:paw size:0.8 legs:4 stride:0.5 ...}. */
    private static final class Rule {
        String id;
        TagKey<EntityType<?>> tag;
        FootShape shape;
        Float size, stride, stance, length;
        Integer legs;

        static Rule parse(String line) {
            int eq = line.indexOf('=');
            if (eq < 0) {
                Footprints.LOGGER.warn("config: profile \"{}\" needs '<mob> = ...'", line);
                return null;
            }
            String key = line.substring(0, eq).trim();
            Rule r = new Rule();
            ResourceLocation id = Footprints.parseId(key.startsWith("#") ? key.substring(1) : key);
            if (id == null) {
                Footprints.LOGGER.warn("config: profile \"{}\": \"{}\" is not a mob id or #tag", line, key);
                return null;
            }
            if (key.startsWith("#")) r.tag = TagKey.create(Registries.ENTITY_TYPE, id);
            else r.id = id.toString();
            for (String part : line.substring(eq + 1).trim().split("\\s+")) {
                if (part.isEmpty()) continue;
                int c = part.indexOf(':');
                String k = c < 0 ? "shape" : part.substring(0, c).trim().toLowerCase(java.util.Locale.ROOT);
                String v = c < 0 ? part : part.substring(c + 1).trim();
                try {
                    switch (k) {
                        case "shape", "texture" -> {
                            r.shape = FootShape.byName(v);
                            if (r.shape == null) Footprints.LOGGER.warn("config: profile \"{}\": no shape or picture \"{}\"", line, v);
                        }
                        case "size" -> r.size = Float.parseFloat(v);
                        case "stride" -> r.stride = Float.parseFloat(v);
                        case "stance" -> r.stance = Float.parseFloat(v);
                        case "length" -> r.length = Float.parseFloat(v);
                        case "legs" -> r.legs = Math.max(2, Math.min(8, Integer.parseInt(v)));
                        default -> Footprints.LOGGER.warn("config: profile \"{}\": unknown \"{}\"", line, k);
                    }
                } catch (NumberFormatException e) {
                    Footprints.LOGGER.warn("config: profile \"{}\": \"{}\" is not a number", line, v);
                }
            }
            return r;
        }
    }
}
