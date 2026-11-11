package online.farmsteadminecraft.footprints.client;

/**
 * One decal on the ground. Mutable and pooled: {@link FootprintPool} reuses instances.
 *
 * <p>A print never moves, so everything the renderer needs except the fade is worked out once, when it
 * is placed ({@link #prepare()}): the four corner offsets and the texture cell. The light level is cached
 * too and looked up again every couple of seconds (torches get placed, the sun sets). Per frame the
 * renderer only subtracts the camera position and writes vertices.</p>
 */
public final class Footprint {
    /** Light is looked up again after this many ticks. */
    private static final int LIGHT_TTL = 40;
    /** The print takes this share of its texture cell; the rest is room for the lip. */
    static final float CELL_FILL = 0.80f;

    public double x, y, z;
    public float yaw;
    public float scale;
    public FootShape shape;
    /** Colour the hollow darkens the ground towards (bluish on snow, warm on sand). */
    public int shadowRgb;
    /** Colour of the displaced lip around the hollow. */
    public int rimRgb;
    public float depth;
    /** How strongly the raised lip shows: 1 for a print pressed into soft ground, 0 for a wet print on stone. */
    public float rim = 1f;
    /** Open to the sky: rain and snowfall wear it away faster. */
    public boolean exposed;
    public long bornTick;
    public int lifeTicks;
    public boolean alive;

    /** Corner offsets from the centre, x/z pairs: toe-left, heel-left, heel-right, toe-right. */
    public final float[] corners = new float[8];
    public float u0, v0, u1, v1;
    public int light;
    public long lightTick = Long.MIN_VALUE;

    /** Call once the placement fields are set. */
    public void prepare() {
        double rad = Math.toRadians(yaw);
        float fx = (float) -Math.sin(rad), fz = (float) Math.cos(rad);
        float rx = (float) -Math.cos(rad), rz = (float) -Math.sin(rad);
        // texture cells are square, the print fills CELL_FILL of the longer side
        float h = Math.max(shape.width, shape.length) * scale * 0.5f / CELL_FILL;
        float ax = (fx - rx) * h, az = (fz - rz) * h;   // toe-left
        float bx = (-fx - rx) * h, bz = (-fz - rz) * h; // heel-left
        corners[0] = ax;
        corners[1] = az;
        corners[2] = bx;
        corners[3] = bz;
        corners[4] = -ax; // heel-right mirrors toe-left
        corners[5] = -az;
        corners[6] = -bx; // toe-right mirrors heel-left
        corners[7] = -bz;
        // the shadow half of the atlas; the lip is the same cell in the right half (+0.5)
        u0 = shape.u0() * 0.5f;
        v0 = shape.v0();
        u1 = shape.u1() * 0.5f;
        v1 = shape.v1();
        lightTick = Long.MIN_VALUE;
    }

    public boolean lightStale(long nowTick) {
        return nowTick - lightTick > LIGHT_TTL;
    }

    /** 1 while fresh, fading to 0 over the last quarter of its life. */
    public float fade(long nowTick) {
        long age = nowTick - bornTick;
        if (age >= lifeTicks) return 0f;
        float fadeStart = lifeTicks * 0.75f;
        if (age <= fadeStart) return 1f;
        float t = (age - fadeStart) / (lifeTicks - fadeStart);
        return 1f - t * t * (3f - 2f * t);
    }
}
