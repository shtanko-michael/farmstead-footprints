package online.farmsteadminecraft.footprints.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import online.farmsteadminecraft.footprints.Footprints;

/**
 * Decides whether a block takes prints and what kind of ground it is, for any block, modded included:
 * <ol>
 *   <li>the exclude list (ids and {@code #tags}) wins;</li>
 *   <li>hand-tuned vanilla surfaces ({@link Surfaces}) keep their colours and lifetimes;</li>
 *   <li>otherwise a block takes prints if it is on the include list or softer than the hardness gate, and
 *       you can stand on it; its sound type says what it is (sand, snow, gravel, mud, grass, wool...), so
 *       a modded sand behaves like sand without any configuration.</li>
 * </ol>
 * The answer is cached per block and forgotten on config reload and when the world changes (tags).
 */
public final class SurfaceRules {
    private static final Surface NONE = new Surface("none", 0f, 0, 0f);
    private static final Map<Block, Surface> CACHE = new IdentityHashMap<>();
    private static Set<ResourceLocation> includeIds, excludeIds;
    private static List<TagKey<Block>> includeTags, excludeTags;

    private SurfaceRules() {
    }

    public static void reset() {
        CACHE.clear();
        includeIds = null;
    }

    /** @return the surface, or {@code null} if the block keeps no prints */
    public static Surface of(BlockState state) {
        Block block = state.getBlock();
        Surface s = CACHE.get(block);
        if (s == null) {
            s = resolve(state);
            CACHE.put(block, s);
        }
        return s == NONE ? null : s;
    }

    /** On the exclude list (ids or tags): no prints of any kind, wet ones included. */
    public static boolean excluded(BlockState state) {
        if (includeIds == null) parseLists();
        return excludeIds.contains(BuiltInRegistries.BLOCK.getKey(state.getBlock())) || anyTag(state, excludeTags);
    }

    private static Surface resolve(BlockState state) {
        if (includeIds == null) parseLists();
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (excludeIds.contains(id) || anyTag(state, excludeTags)) return NONE;
        Surface known = Surfaces.byId(id.toString());
        if (known != null) return known;
        boolean listed = includeIds.contains(id) || anyTag(state, includeTags);
        float hardness = state.getBlock().defaultDestroyTime();
        boolean soft = hardness >= 0f && hardness < FootprintsConfig.HARDNESS.floatValue();
        if (!listed && !soft) return NONE;
        if (!state.isSolid()) return NONE; // flowers, tall grass: nothing to stand on
        return bySound(state.getSoundType());
    }

    private static Surface bySound(SoundType sound) {
        if (sound == SoundType.SAND || sound == SoundType.SUSPICIOUS_SAND) return Surface.SAND;
        if (sound == SoundType.SNOW || sound == SoundType.POWDER_SNOW) return Surface.SNOW;
        if (sound == SoundType.GRAVEL || sound == SoundType.SUSPICIOUS_GRAVEL) return Surface.GRAVEL;
        if (sound == SoundType.MUD || sound == SoundType.MUDDY_MANGROVE_ROOTS) return Surface.MUD;
        if (sound == SoundType.GRASS || sound == SoundType.MOSS || sound == SoundType.MOSS_CARPET) return Surface.GRASS;
        if (sound == SoundType.ROOTED_DIRT || sound == SoundType.NYLIUM) return Surface.SOIL;
        if (sound == SoundType.WOOL) return Surface.CLOTH;
        if (sound == SoundType.SOUL_SAND || sound == SoundType.SOUL_SOIL) return Surface.SOUL;
        return Surface.SOFT;
    }

    private static boolean anyTag(BlockState state, List<TagKey<Block>> tags) {
        for (TagKey<Block> tag : tags) {
            if (state.is(tag)) return true;
        }
        return false;
    }

    private static void parseLists() {
        Set<ResourceLocation> inIds = new HashSet<>(), exIds = new HashSet<>();
        List<TagKey<Block>> inTags = new ArrayList<>(), exTags = new ArrayList<>();
        parse(FootprintsConfig.SURFACE_INCLUDE.get(), inIds, inTags);
        parse(FootprintsConfig.SURFACE_EXCLUDE.get(), exIds, exTags);
        includeIds = inIds;
        excludeIds = exIds;
        includeTags = inTags;
        excludeTags = exTags;
    }

    private static void parse(List<String> entries, Set<ResourceLocation> ids, List<TagKey<Block>> tags) {
        for (String entry : entries) {
            String e = entry.trim();
            boolean tag = e.startsWith("#");
            ResourceLocation id = Footprints.parseId(tag ? e.substring(1) : e);
            if (id == null) {
                Footprints.LOGGER.warn("config: \"{}\" is not a block id or #tag", entry);
            } else if (tag) {
                tags.add(TagKey.create(Registries.BLOCK, id));
            } else {
                ids.add(id);
            }
        }
    }
}
