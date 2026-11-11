package online.farmsteadminecraft.imprint.client;

/**
 * A footprint mask in {@code textures/footprints.png} (4x4 grid of 32x32 cells)
 * and its size in blocks. Width is across the foot, length along the walking direction.
 */
public enum FootShape {
    BOOT_LEFT(0, 0.32f, 0.57f),
    BOOT_RIGHT(1, 0.32f, 0.57f),
    BARE_LEFT(2, 0.30f, 0.51f),
    BARE_RIGHT(3, 0.30f, 0.51f),
    PAW(4, 0.32f, 0.32f),
    HOOF(5, 0.35f, 0.38f),
    BIRD(6, 0.32f, 0.32f),
    BIG_LEFT(7, 0.57f, 0.68f),
    BIG_RIGHT(8, 0.57f, 0.68f);

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
