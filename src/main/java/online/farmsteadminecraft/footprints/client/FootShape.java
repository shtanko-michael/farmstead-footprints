package online.farmsteadminecraft.footprints.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A footprint mask: one cell of the runtime atlas ({@link FootprintAtlas}) and its size in blocks.
 * Width is across the foot, length along the walking direction.
 *
 * <p>The built-in shapes are cells 0–12 of the mod's own atlas. Pictures players drop into
 * {@code config/farmstead_footprints/textures/} become shapes too, named after the file, and take the
 * cells from {@link #FIRST_CUSTOM_CELL} on; config profiles refer to any shape by name.</p>
 */
public final class FootShape {
    private static final Map<String, FootShape> BY_NAME = new LinkedHashMap<>();
    private static final List<FootShape> BUILT_IN = new ArrayList<>();

    // width/length follow each mask's aspect ratio (tools/build_footprints.py prints them)
    public static final FootShape BOOT_LEFT = builtIn("boot_left", 0, 0.31f, 0.55f);
    public static final FootShape BOOT_RIGHT = builtIn("boot_right", 1, 0.31f, 0.55f);
    public static final FootShape BARE_LEFT = builtIn("bare_left", 2, 0.25f, 0.50f);
    public static final FootShape BARE_RIGHT = builtIn("bare_right", 3, 0.25f, 0.50f);
    public static final FootShape PAW = builtIn("paw", 4, 0.29f, 0.30f);
    public static final FootShape HOOF = builtIn("hoof", 5, 0.34f, 0.34f);
    public static final FootShape BIRD = builtIn("bird", 6, 0.27f, 0.32f);
    public static final FootShape BIG_LEFT = builtIn("big_left", 7, 0.64f, 0.70f);
    public static final FootShape BIG_RIGHT = builtIn("big_right", 8, 0.64f, 0.70f);
    public static final FootShape CAT = builtIn("cat", 9, 0.23f, 0.20f);
    public static final FootShape CLOVEN = builtIn("cloven", 10, 0.26f, 0.27f);
    public static final FootShape BEAR = builtIn("bear", 11, 0.50f, 0.48f);
    /** A shuffling, shallow print: anything that sneaks. */
    public static final FootShape SNEAK = builtIn("sneak", 12, 0.26f, 0.50f);
    /** Foam chevron behind a boat (drawn at runtime, {@link FootprintAtlas}). */
    public static final FootShape WAKE = builtIn("wake", 13, 1.0f, 1.0f);
    /** A ring on the water where something wades. */
    public static final FootShape RIPPLE = builtIn("ripple", 14, 1.0f, 1.0f);
    /** Cells taken by the built-in shapes; custom pictures start here. */
    public static final int FIRST_CUSTOM_CELL = 16;

    public final String name;
    public final int cell;
    public final float width;
    public final float length;

    private FootShape(String name, int cell, float width, float length) {
        this.name = name;
        this.cell = cell;
        this.width = width;
        this.length = length;
    }

    private static FootShape builtIn(String name, int cell, float width, float length) {
        FootShape s = new FootShape(name, cell, width, length);
        BY_NAME.put(name, s);
        BUILT_IN.add(s);
        return s;
    }

    /** Registers a player's picture as a shape; replaces an older one of that name. */
    static FootShape custom(String name, int cell, float width, float length) {
        FootShape s = new FootShape(name.toLowerCase(Locale.ROOT), cell, width, length);
        BY_NAME.put(s.name, s);
        return s;
    }

    /** Drops every custom shape (before the atlas is rebuilt). */
    static void clearCustom() {
        BY_NAME.values().removeIf(s -> s.cell >= FIRST_CUSTOM_CELL);
    }

    /** By name, case-insensitive; null if unknown. "boot", "bare" and "big" mean the left foot of the pair. */
    public static FootShape byName(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        FootShape s = BY_NAME.get(n);
        return s != null ? s : BY_NAME.get(n + "_left");
    }

    /** The mirror partner for pairs (boot, bare, big); the shape itself otherwise. */
    public FootShape right() {
        if (this == BOOT_LEFT) return BOOT_RIGHT;
        if (this == BARE_LEFT) return BARE_RIGHT;
        if (this == BIG_LEFT) return BIG_RIGHT;
        return this;
    }

    public static List<FootShape> builtIns() {
        return Collections.unmodifiableList(BUILT_IN);
    }

    public float u0() {
        return (cell % FootprintAtlas.grid()) / (float) FootprintAtlas.grid();
    }

    public float v0() {
        return (cell / FootprintAtlas.grid()) / (float) FootprintAtlas.grid();
    }

    public float u1() {
        return u0() + 1f / FootprintAtlas.grid();
    }

    public float v1() {
        return v0() + 1f / FootprintAtlas.grid();
    }

    @Override
    public String toString() {
        return name;
    }
}
