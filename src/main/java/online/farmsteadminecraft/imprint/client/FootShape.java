package online.farmsteadminecraft.imprint.client;

/**
 * A footprint mask in {@code textures/footprints.png} (4x4 grid of 32x32 cells)
 * and its size in blocks. Width is across the foot, length along the walking direction.
 */
public enum FootShape {
    // width/length follow each mask's aspect ratio (tools/build_footprints.py prints them)
    BOOT_LEFT(0, 0.31f, 0.55f),
    BOOT_RIGHT(1, 0.31f, 0.55f),
    BARE_LEFT(2, 0.25f, 0.50f),
    BARE_RIGHT(3, 0.25f, 0.50f),
    PAW(4, 0.29f, 0.30f),
    HOOF(5, 0.34f, 0.34f),
    BIRD(6, 0.27f, 0.32f),
    BIG_LEFT(7, 0.64f, 0.70f),
    BIG_RIGHT(8, 0.64f, 0.70f),
    CAT(9, 0.23f, 0.20f),
    CLOVEN(10, 0.26f, 0.27f),
    BEAR(11, 0.50f, 0.48f),
    /** A shuffling, shallow print: anything that sneaks. */
    SNEAK(12, 0.26f, 0.50f);

    public static final int GRID = 4;

    public final int cell;
    public final float width;
    public final float length;

    FootShape(int cell, float width, float length) {
        this.cell = cell;
        this.width = width;
        this.length = length;
    }

    public float u0() {
        return (cell % GRID) / (float) GRID;
    }

    public float v0() {
        return (cell / GRID) / (float) GRID;
    }

    public float u1() {
        return u0() + 1f / GRID;
    }

    public float v1() {
        return v0() + 1f / GRID;
    }
}
