package online.farmsteadminecraft.footprints.client;

/**
 * How a kind of entity walks and what its feet leave behind. All distances are blocks for an adult
 * (babies are scaled by the entity's scale).
 *
 * <p>Two-legged walkers alternate left and right {@code stride} apart, {@code spacing} off the
 * centre line.</p>
 *
 * <p>Four-legged (and six-legged) walkers follow real track patterns: in a walk or trot the feet of
 * one side come down one after another in the same place — the hind foot lands in the print the front
 * foot of that side just left ("direct register"). So the trail is a single zig-zag line of prints,
 * left, right, left, as wide as the animal's stance and no wider. {@code legs} are the hip positions
 * of one side taken from the vanilla model, front first: {lateral, forward, lateral, forward, ...}.
 * A hind foot lands {@code register} ahead of the front print (bears overstep a little, so their hind
 * print sits just in front of and outside the front one), and at its own lateral offset.</p>
 *
 * @param left      mask for the left foot
 * @param right     mask for the right foot
 * @param stride    blocks between two successive prints (left to right) at walking pace
 * @param spacing   two-legged: sideways distance of a foot from the centre line
 * @param legs      four-legged: hips of one side, front first, as {lateral, forward} pairs; null for bipeds
 * @param register  four-legged: how far ahead of the front print the hind feet land
 * @param toeIn     degrees the toes turn towards the centre line (bears walk pigeon-toed)
 * @param scale     size multiplier for the mask
 */
public record FootProfile(FootShape left, FootShape right, float stride, float spacing, float[] legs, float register,
                          float toeIn, float scale) {

    public boolean quadruped() {
        return legs != null;
    }

    static FootProfile biped(FootShape left, FootShape right, float stride, float spacing, float scale) {
        return new FootProfile(left, right, stride, spacing, null, 0f, 0f, scale);
    }

    /** {@code legs}: model pivots of the left legs in model pixels (x, z), front first; {@code model} the renderer scale. */
    static FootProfile quad(FootShape shape, float scale, float stride, float register, float toeIn, float model, float... legs) {
        float[] blocks = new float[legs.length];
        for (int i = 0; i < legs.length; i += 2) {
            blocks[i] = Math.abs(legs[i]) / 16f * model;   // lateral
            blocks[i + 1] = -legs[i + 1] / 16f * model;     // model -z is forward
        }
        return new FootProfile(shape, shape, stride, 0f, blocks, register, toeIn, scale);
    }

    public static final FootProfile BOOTS = biped(FootShape.BOOT_LEFT, FootShape.BOOT_RIGHT, 0.92f, 0.13f, 1f);
    public static final FootProfile BAREFOOT = biped(FootShape.BARE_LEFT, FootShape.BARE_RIGHT, 0.92f, 0.13f, 1f);
    public static final FootProfile CHILD = biped(FootShape.BARE_LEFT, FootShape.BARE_RIGHT, 0.55f, 0.08f, 0.6f);
    public static final FootProfile BIRD = biped(FootShape.BIRD, FootShape.BIRD, 0.30f, 0.09f, 0.8f);
    /** Iron golems and wardens: legs about 4.5–6 model pixels off the centre. */
    public static final FootProfile GIANT = biped(FootShape.BIG_LEFT, FootShape.BIG_RIGHT, 1.3f, 0.3f, 1f);

    // Hip positions below are the vanilla model's leg pivots (client/model/*Model.java); the stride is
    // about 0.7 of the distance between front and hind hips, as in a real walk with direct register.
    public static final FootProfile WOLF = quad(FootShape.PAW, 0.85f, 0.48f, 0f, 0f, 1f, 1.5f, -4f, 1.5f, 7f);
    public static final FootProfile FOX = quad(FootShape.PAW, 0.7f, 0.34f, 0f, 0f, 1f, 2f, 0f, 2f, 7f);
    public static final FootProfile ARMADILLO = quad(FootShape.PAW, 0.6f, 0.3f, 0f, 0f, 1f, 2f, -4f, 2f, 4f);
    public static final FootProfile CAT = quad(FootShape.CAT, 0.85f, 0.34f, 0f, 0f, 0.8f, 1.2f, -5f, 1.1f, 5f);
    public static final FootProfile OCELOT = quad(FootShape.CAT, 0.9f, 0.4f, 0f, 0f, 1f, 1.2f, -5f, 1.1f, 5f);
    public static final FootProfile CREEPER = quad(FootShape.CAT, 0.9f, 0.32f, 0f, 0f, 1f, 2f, -4f, 2f, 4f);
    public static final FootProfile HORSE = quad(FootShape.HOOF, 1.0f, 0.85f, 0f, 0f, 1.1f, 4f, -12f, 4f, 7f);
    public static final FootProfile DONKEY = quad(FootShape.HOOF, 0.85f, 0.68f, 0f, 0f, 0.87f, 4f, -12f, 4f, 7f);
    public static final FootProfile MULE = quad(FootShape.HOOF, 0.9f, 0.72f, 0f, 0f, 0.92f, 4f, -12f, 4f, 7f);
    public static final FootProfile COW = quad(FootShape.CLOVEN, 1.3f, 0.56f, 0f, 0f, 1f, 4f, -6f, 4f, 7f);
    public static final FootProfile LLAMA = quad(FootShape.CLOVEN, 1.1f, 0.48f, 0f, 0f, 1f, 3.5f, -5f, 3.5f, 6f);
    public static final FootProfile PIG = quad(FootShape.CLOVEN, 0.95f, 0.5f, 0f, 0f, 1f, 3f, -5f, 3f, 7f);
    public static final FootProfile GOAT = quad(FootShape.CLOVEN, 0.9f, 0.42f, 0f, 0f, 1f, 2f, -4.5f, 2f, 5.5f);
    public static final FootProfile HOGLIN = quad(FootShape.CLOVEN, 1.5f, 0.78f, 0f, 0f, 1f, 4f, -8.5f, 5f, 10f);
    /** Bears overstep slightly and walk pigeon-toed: the hind print lands just ahead of and outside the front one. */
    public static final FootProfile POLAR_BEAR = quad(FootShape.BEAR, 1.0f, 0.68f, 0.12f, 12f, 1.2f, 3.5f, -8f, 4.5f, 6f);
    public static final FootProfile PANDA = quad(FootShape.BEAR, 1.0f, 0.72f, 0.1f, 10f, 1f, 5.5f, -9f, 5.5f, 9f);
    /** Camels have two broad toes on a soft pad: the cloven mask, large, on long legs. */
    public static final FootProfile CAMEL = quad(FootShape.CLOVEN, 1.75f, 1.05f, 0f, 0f, 1f, 4.9f, -10.5f, 4.9f, 9.5f);
    /** Six legs: the middle and hind feet both land in the front print. */
    public static final FootProfile SNIFFER = quad(FootShape.CLOVEN, 1.6f, 1.2f, 0f, 0f, 1f, 7.5f, -15f, 7.5f, 0f, 7.5f, 15f);
    public static final FootProfile RAVAGER = quad(FootShape.HOOF, 1.6f, 0.95f, 0f, 0f, 1f, 8f, -5f, 8f, 18f);
}
