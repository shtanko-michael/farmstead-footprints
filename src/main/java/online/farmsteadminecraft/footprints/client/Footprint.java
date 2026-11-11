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
    /** Texture offset from one decay stage to the next. */
    public float stageStep;
    /** The block the print is pressed into ({@code BlockPos.asLong}) and which block it was. */
    public long supportPos;
    public net.minecraft.world.level.block.Block supportBlock;
    /** Water marks spread out: the size grows by this share of itself over the mark's life. */
    public float grow;
    /** Lies on water: drawn after the water ({@link online.farmsteadminecraft.footprints.client.render.FootprintRenderer#PASS_WATER}). */
    public boolean onWater;
    /** Half the side of the print's square, in blocks. */
    public float half;

    /** Pixel style ({@link PixelStyle}): the ground's top face could be read, so this print can be drawn from it. */
    public boolean pixel;
    /** Block texture coordinates as a + b*x + c*z of the block-local position (0..1) on the top face. */
    public final float[] uMap = new float[3], vMap = new float[3];
    public int tint = 0xFFFFFF;
    /** Texel runs of the print for {@link #stripStage}: start | length << 8 | row << 16 | shade << 24. */
    public int[] strips = new int[24];
    public int stripCount;
    public int stripStage = -1;
    /** World texel (1/16 block) of the runs' origin. */
    public int texX, texZ;
    public int light;
    public long lightTick = Long.MIN_VALUE;

    /** Call once the placement fields are set. */
    public void prepare() {
        double rad = Math.toRadians(yaw);
        float fx = (float) -Math.sin(rad), fz = (float) Math.cos(rad);
        float rx = (float) -Math.cos(rad), rz = (float) -Math.sin(rad);
        // texture cells are square, the print fills CELL_FILL of the longer side
        float h = Math.max(shape.width, shape.length) * scale * 0.5f / CELL_FILL;
        half = h;
        stripStage = -1;
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
        u1 = shape.u1() * 0.5f;
        // stage 0 (fresh) is the top band of the atlas; worn stages lie below it, one band each
        float band = 1f / FootprintAtlas.stages();
        v0 = shape.v0() * band;
        v1 = shape.v1() * band;
        stageStep = band;
        lightTick = Long.MIN_VALUE;
    }

    public boolean lightStale(long nowTick) {
        return lightTick == Long.MIN_VALUE || nowTick - lightTick > LIGHT_TTL;
    }

    /**
     * 1 while fresh, then down to 0 at the end of its life. With crumbling decay the print wears from the edges over
     * the second half of its life ({@link #stage}) and only the last remains fade out; plain decay fades it over the
     * last quarter.
     */
    public float fade(long nowTick, boolean crumble) {
        long age = nowTick - bornTick;
        if (age >= lifeTicks) return 0f;
        float fadeStart = lifeTicks * (crumble ? 0.9f : 0.75f);
        if (age <= fadeStart) return 1f;
        float t = (age - fadeStart) / (lifeTicks - fadeStart);
        return 1f - t * t * (3f - 2f * t);
    }

    /** Which worn copy of the print to draw: 0 for the first half of its life, then step by step to the last. */
    public int stage(long nowTick, int stages) {
        float progress = ((nowTick - bornTick) / (float) lifeTicks - 0.5f) * 2f;
        if (progress <= 0f) return 0;
        return Math.min(stages - 1, 1 + (int) (progress * (stages - 1)));
    }
}
