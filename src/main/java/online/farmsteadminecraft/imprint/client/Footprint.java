package online.farmsteadminecraft.imprint.client;

/** One decal on the ground. Mutable and pooled: {@link FootprintPool} reuses instances. */
public final class Footprint {
    public double x, y, z;
    public float yaw;
    public float scale;
    public FootShape shape;
    public int rgb;
    public float depth;
    public long bornTick;
    public int lifeTicks;
    public boolean alive;

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
