package online.farmsteadminecraft.footprints.client;

/**
 * How a block takes footprints.
 *
 * @param depth     opacity of a fresh print, 0..1
 * @param lifeSecs  seconds a print stays (before the lifetime multiplier)
 * @param darken    how much darker than the block colour the print is, 0..1
 */
public record Surface(String name, float depth, int lifeSecs, float darken) {
    public static final Surface SNOW = new Surface("snow", 0.80f, 300, 0.42f);
    public static final Surface SAND = new Surface("sand", 0.78f, 180, 0.40f);
    public static final Surface MUD = new Surface("mud", 0.85f, 240, 0.55f);
    public static final Surface GRAVEL = new Surface("gravel", 0.60f, 90, 0.45f);
    public static final Surface SOIL = new Surface("soil", 0.70f, 120, 0.45f);
    public static final Surface CLAY = new Surface("clay", 0.72f, 200, 0.40f);
    public static final Surface SOUL = new Surface("soul", 0.70f, 150, 0.50f);
}
