package online.farmsteadminecraft.imprint.client;

/**
 * How a kind of entity walks.
 *
 * @param left       mask for the left foot
 * @param right      mask for the right foot
 * @param stride     blocks travelled between two prints
 * @param spacing    sideways distance of a foot from the body centre
 * @param quadruped  four-legged: each step leaves a front and a back print
 * @param scale      size multiplier for the mask
 */
public record FootProfile(FootShape left, FootShape right, float stride, float spacing, boolean quadruped, float scale) {
    public static final FootProfile BOOTS = new FootProfile(FootShape.BOOT_LEFT, FootShape.BOOT_RIGHT, 0.92f, 0.13f, false, 1f);
    public static final FootProfile BAREFOOT = new FootProfile(FootShape.BARE_LEFT, FootShape.BARE_RIGHT, 0.92f, 0.13f, false, 1f);
    public static final FootProfile CHILD = new FootProfile(FootShape.BARE_LEFT, FootShape.BARE_RIGHT, 0.55f, 0.08f, false, 0.6f);
    public static final FootProfile PAW = new FootProfile(FootShape.PAW, FootShape.PAW, 0.62f, 0.12f, true, 1f);
    public static final FootProfile SMALL_PAW = new FootProfile(FootShape.PAW, FootShape.PAW, 0.42f, 0.09f, true, 0.7f);
    public static final FootProfile HOOF = new FootProfile(FootShape.HOOF, FootShape.HOOF, 0.95f, 0.22f, true, 1.1f);
    public static final FootProfile SMALL_HOOF = new FootProfile(FootShape.HOOF, FootShape.HOOF, 0.6f, 0.16f, true, 0.85f);
    public static final FootProfile BIRD = new FootProfile(FootShape.BIRD, FootShape.BIRD, 0.30f, 0.09f, false, 0.8f);
    public static final FootProfile GIANT = new FootProfile(FootShape.BIG_LEFT, FootShape.BIG_RIGHT, 1.45f, 0.42f, false, 1f);
    public static final FootProfile GIANT_PAW = new FootProfile(FootShape.PAW, FootShape.PAW, 1.1f, 0.38f, true, 2.2f);
}
