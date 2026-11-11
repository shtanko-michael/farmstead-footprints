package online.farmsteadminecraft.footprints.client;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Turns entity movement into steps. Each tracked entity accumulates the horizontal distance it
 * walks on the ground; every {@link FootProfile#stride()} blocks a print is placed under the
 * next foot. Works for any entity the client sees, with no server support.
 */
public final class StepTracker {
    private static final int FORGET_AFTER_TICKS = 100;
    private static final double TELEPORT_DISTANCE = 2.5;

    private final Int2ObjectMap<Track> tracks = new Int2ObjectOpenHashMap<>();
    private final FootprintPool pool;
    private final java.util.Random random = new java.util.Random();
    /** Wetness of the entity being stepped right now, 0..1 (set per entity in tick). */
    private float wetNow;

    public StepTracker(FootprintPool pool) {
        this.pool = pool;
    }

    private static final class Track {
        double lastX, lastZ;
        /** Ticks the feet stay wet after water or rain. */
        int wet;
        double walked;
        boolean right;
        long seen;
        boolean valid;
        /** Distance walked since the track started; hind prints wait for it. */
        double odometer;
        /** Four-legged: the trailing feet still to come down in the last front print of each side. */
        final java.util.List<HindStep> hind = new java.util.ArrayList<>(4);
    }

    /** A trailing foot that comes down in the front print of its side once its hip has reached it. */
    private record HindStep(boolean right, double x, double z, float yaw, FootShape shape, double dueAt) {}

    public void tick(ClientLevel level, Vec3 camera, long now) {
        double maxDistSq = (double) FootprintsConfig.RENDER_DISTANCE.value() * FootprintsConfig.RENDER_DISTANCE.value();
        for (Entity e : level.entitiesForRendering()) {
            if (!(e instanceof LivingEntity living)) continue;
            if (e.position().distanceToSqr(camera) > maxDistSq) continue;
            Track track = tracks.computeIfAbsent(e.getId(), id -> new Track());
            track.seen = now;
            int wetTicks = FootprintsConfig.WET_SECONDS.value() * 20;
            if (living.isInWaterOrRain()) track.wet = wetTicks;
            else if (track.wet > 0) track.wet--;
            wetNow = FootprintsConfig.WET_PRINTS.on() ? track.wet / (float) wetTicks : 0f;
            if (!canStep(level, living)) {
                track.valid = false;
                continue;
            }
            double x = e.getX();
            double z = e.getZ();
            if (!track.valid) {
                track.lastX = x;
                track.lastZ = z;
                track.walked = 0;
                track.hind.clear();
                track.valid = true;
                continue;
            }
            double moved = Math.sqrt(Mth.square(x - track.lastX) + Mth.square(z - track.lastZ));
            track.lastX = x;
            track.lastZ = z;
            if (moved > TELEPORT_DISTANCE) {
                track.walked = 0;
                track.hind.clear();
                continue;
            }
            FootProfile profile = FootProfiles.of(living);
            if (profile == null || sneakHidden(living)) continue;
            track.walked += moved;
            track.odometer += moved;
            for (java.util.Iterator<HindStep> it = track.hind.iterator(); it.hasNext(); ) {
                HindStep h = it.next();
                if (track.odometer >= h.dueAt()) {
                    it.remove();
                    place(level, living, profile, h.shape(), h.x(), h.z(), h.yaw(), now);
                }
            }
            // Steps lengthen with speed, like a walk turning into a trot (per tick: 0.1 is a stroll, 0.3+ a run)
            float pace = Mth.clamp(0.85f + 1.5f * (float) moved, 0.9f, 1.5f);
            float stride = profile.stride() * pace * (profile == FootProfile.CHILD ? 1f : scaleOf(living));
            if (track.walked >= stride) {
                track.walked -= stride;
                if (track.walked > stride) track.walked = 0;
                // Hoppers leave one group of four per bound. Client-side, other mobs' small jumps barely show
                // in their position, so a bound is measured by distance, like a step.
                if (profile.hop()) {
                    land(level, living, profile, now);
                    continue;
                }
                step(level, living, profile, track, track.right, now);
                track.right = !track.right;
            }
        }
        if (now % 20 == 0) {
            tracks.int2ObjectEntrySet().removeIf(entry -> now - entry.getValue().seen > FORGET_AFTER_TICKS);
        }
    }

    private static boolean canStep(ClientLevel level, LivingEntity e) {
        if (e.isPassenger() || e.isInWater() || e.isSpectator() || e.isInLava()) return false;
        return e.onGround() || restsOnSurface(level, e);
    }

    /**
     * Fallback for entities the client does not simulate itself: server-driven movement
     * (teleports, lag corrections, some remote players) can leave {@code onGround} false while
     * the feet are exactly on top of a block.
     */
    private static boolean restsOnSurface(ClientLevel level, LivingEntity e) {
        if (Math.abs(e.getDeltaMovement().y) > 0.1) return false;
        BlockPos below = BlockPos.containing(e.getX(), e.getY() - 0.05, e.getZ());
        net.minecraft.world.phys.shapes.VoxelShape shape = level.getBlockState(below).getCollisionShape(level, below);
        if (shape.isEmpty()) return false;
        double top = below.getY() + shape.max(net.minecraft.core.Direction.Axis.Y);
        return Math.abs(e.getY() - top) < 0.02;
    }

    /**
     * How much bigger or smaller this entity is than its kind: babies, the scale attribute, and size mods
     * (Pehkui, Random Mob Sizes) all change the hitbox, so its width against the type's says it, without
     * depending on any of them.
     */
    static float scaleOf(LivingEntity e) {
        float base = e.getType().getWidth();
        return base <= 0f ? 1f : Mth.clamp(e.getBbWidth() / base, 0.2f, 8f);
    }

    /** Sneaking with {@code footprints.sneaking = none}: no prints at all. */
    private static boolean sneakHidden(LivingEntity e) {
        return e.isShiftKeyDown() && FootprintsConfig.SNEAKING.get() == FootprintsConfig.Sneaking.NONE;
    }

    /**
     * A hop (rabbits, frogs): the long hind feet land side by side ahead of the front feet, which come down
     * one behind the other on the centre line — the "Y" of a rabbit track.
     */
    private void land(ClientLevel level, LivingEntity e, FootProfile profile, long now) {
        float yaw = e.yBodyRot;
        double rad = Math.toRadians(yaw);
        double fx = -Math.sin(rad), fz = Math.cos(rad);
        double rx = -Math.cos(rad), rz = -Math.sin(rad);
        float k = scaleOf(e);
        float[] legs = profile.legs();
        double hindLat = (legs != null ? legs[legs.length - 2] : 0.1) * k;
        double hindFwd = 0.12 * k, front1 = -0.12 * k, front2 = -0.3 * k;
        FootShape shape = profile.left();
        place(level, e, profile, shape, e.getX() + rx * hindLat + fx * hindFwd, e.getZ() + rz * hindLat + fz * hindFwd, yaw, now, 1f);
        place(level, e, profile, shape, e.getX() - rx * hindLat + fx * hindFwd, e.getZ() - rz * hindLat + fz * hindFwd, yaw, now, 1f);
        place(level, e, profile, shape, e.getX() + rx * 0.03 * k + fx * front1, e.getZ() + rz * 0.03 * k + fz * front1, yaw, now, 0.7f);
        place(level, e, profile, shape, e.getX() - rx * 0.03 * k + fx * front2, e.getZ() - rz * 0.03 * k + fz * front2, yaw, now, 0.7f);
    }

    private void step(ClientLevel level, LivingEntity e, FootProfile profile, Track track, boolean right, long now) {
        float yaw = e.getYRot();
        if (!(e instanceof net.minecraft.world.entity.player.Player)) yaw = e.yBodyRot;
        double rad = Math.toRadians(yaw);
        // Minecraft yaw 0 faces +Z; right of that is -X
        double fx = -Math.sin(rad), fz = Math.cos(rad);
        double rx = -Math.cos(rad), rz = -Math.sin(rad);
        double sign = right ? 1 : -1;
        FootShape shape = right ? profile.right() : profile.left();
        if (!profile.quadruped()) {
            double side = sign * profile.spacing() * (profile == FootProfile.CHILD ? 1f : scaleOf(e));
            place(level, e, profile, shape, e.getX() + rx * side, e.getZ() + rz * side, yaw, now);
            return;
        }
        // The front foot of this side comes down a little ahead of its hip; every foot behind it on the
        // same side will land in that same print (offset by the register and its own stance width) once
        // its hip has travelled the distance between the two hips. Trailing feet of this side that have
        // not landed yet do so now: a foot is never dropped.
        float k = scaleOf(e);
        float[] legs = profile.legs();
        double lat0 = legs[0] * k, fwd0 = legs[1] * k;
        double ahead = profile.stride() * k * 0.3;
        double px = e.getX() + rx * sign * lat0 + fx * (fwd0 + ahead);
        double pz = e.getZ() + rz * sign * lat0 + fz * (fwd0 + ahead);
        float toe = yaw - (float) sign * profile.toeIn(); // toes turned towards the centre line
        for (java.util.Iterator<HindStep> it = track.hind.iterator(); it.hasNext(); ) {
            HindStep h = it.next();
            if (h.right() == right) {
                it.remove();
                place(level, e, profile, h.shape(), h.x(), h.z(), h.yaw(), now);
            }
        }
        place(level, e, profile, shape, px, pz, toe, now);
        double reg = profile.register() * k;
        for (int i = 2; i < legs.length; i += 2) {
            double lat = legs[i] * k, fwd = legs[i + 1] * k;
            double hx = px + rx * sign * (lat - lat0) + fx * reg;
            double hz = pz + rz * sign * (lat - lat0) + fz * reg;
            track.hind.add(new HindStep(right, hx, hz, toe, shape, track.odometer + (fwd0 - fwd) + reg));
        }
    }

    private void place(ClientLevel level, LivingEntity e, FootProfile profile, FootShape shape, double x, double z, float yaw, long now) {
        place(level, e, profile, shape, x, z, yaw, now, 1f);
    }

    private void place(ClientLevel level, LivingEntity e, FootProfile profile, FootShape shape, double x, double z, float yaw,
                       long now, float sizeMul) {
        double y = e.getY();
        // A thin cover (a snow layer) can sit in the block the feet are in; the print belongs on top of
        // it, not on the block underneath, or the cover hides it. Server-placed entities and camera-rig
        // actors often stand at the block's integer top with the layer around their feet.
        BlockPos pos = BlockPos.containing(x, y + 0.01, z);
        BlockState state = level.getBlockState(pos);
        Surface surface = SurfaceRules.of(state);
        if (surface == null) {
            pos = BlockPos.containing(x, y - 0.05, z);
            state = level.getBlockState(pos);
            surface = SurfaceRules.of(state);
        }
        boolean wetOnHard = false;
        if (surface == null) {
            // a wet foot marks hard ground too: whatever solid block it stands on, unless excluded
            if (wetNow <= 0.05f) return;
            pos = BlockPos.containing(x, y - 0.05, z);
            state = level.getBlockState(pos);
            if (!state.isSolid() || SurfaceRules.excluded(state)) return;
            surface = Surface.WET;
            wetOnHard = true;
        }
        net.minecraft.world.phys.shapes.VoxelShape outline = state.getShape(level, pos);
        double top = outline.isEmpty() ? pos.getY() : pos.getY() + outline.max(net.minecraft.core.Direction.Axis.Y);
        Footprint p = pool.add();
        p.grow = 0f;
        p.onWater = false;
        // no two prints alike: a few degrees, a few percent, a few millimetres
        p.x = x + (random.nextFloat() - 0.5f) * 0.06f;
        p.y = Math.max(top, Math.min(y, top + 0.2));
        p.z = z + (random.nextFloat() - 0.5f) * 0.06f;
        p.yaw = yaw + (random.nextFloat() - 0.5f) * 14f;
        p.shape = e.isShiftKeyDown() && !profile.quadruped() ? FootShape.SNEAK : shape;
        p.scale = profile.scale() * sizeMul * (profile == FootProfile.CHILD ? 1f : scaleOf(e)) * (0.92f + random.nextFloat() * 0.16f)
            * FootprintsConfig.SIZE.floatValue();
        p.supportPos = pos.asLong();
        p.supportBlock = state.getBlock();
        p.depth = surface.depth() * (e.isShiftKeyDown() ? 0.55f : 1f) * (0.85f + random.nextFloat() * 0.2f)
            * (e.isSprinting() ? 1.1f : 1f);
        int ground = state.getMapColor(level, pos).col;
        if (wetOnHard) {
            // water darkens stone and wood; no lip, and it fades as the foot dries
            p.depth *= Math.min(1f, wetNow * 1.4f) * FootprintsConfig.WET_OPACITY.floatValue();
            p.shadowRgb = scale(ground, 0.45f, 0);
            p.rimRgb = ground;
            p.rim = 0f;
        } else {
            p.shadowRgb = shadowColour(ground, surface);
            // still-wet feet leave a darker print on soft ground too
            if (wetNow > 0.05f) p.shadowRgb = scale(p.shadowRgb, 1f - 0.25f * wetNow, 0);
            p.rimRgb = rimColour(ground);
            p.rim = 1f;
        }
        p.exposed = level.canSeeSky(pos.above());
        p.bornTick = now;
        p.lifeTicks = (int) Math.max(40, surface.lifeSecs() * 20 * FootprintsConfig.LIFETIME.value()
            * (0.9f + random.nextFloat() * 0.2f));
        p.prepare();
        try {
            p.pixel = PixelStyle.capture(p, level, pos, state);
        } catch (RuntimeException ex) {
            // an odd modded model: this print keeps the detailed look
            p.pixel = false;
        }
        dust(level, e, state, surface, p.x, p.y, p.z);
        if (wetNow > 0.3f) WaterEffects.splash(level, e, p.x, p.y, p.z, wetNow);
    }

    /** A puff from under the foot on loose ground (snow, sand, gravel, soil), more when running. */
    private void dust(ClientLevel level, LivingEntity e, BlockState state, Surface surface, double x, double y, double z) {
        FootprintsConfig.Dust mode = FootprintsConfig.DUST.get();
        if (mode == FootprintsConfig.Dust.OFF) return;
        if (mode == FootprintsConfig.Dust.PLAYERS && !(e instanceof net.minecraft.world.entity.player.Player)) return;
        if (surface != Surface.SNOW && surface != Surface.SAND && surface != Surface.GRAVEL && surface != Surface.SOIL
            && surface != Surface.SOUL) return;
        int n = e.isSprinting() ? 3 : random.nextFloat() < 0.35f ? 1 : 0;
        for (int i = 0; i < n; i++) {
            level.addParticle(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, state),
                x + (random.nextDouble() - 0.5) * 0.2, y + 0.05, z + (random.nextDouble() - 0.5) * 0.2,
                (random.nextDouble() - 0.5) * 0.08, 0.08 + random.nextDouble() * 0.06, (random.nextDouble() - 0.5) * 0.08);
        }
    }

    private static float luminance(int rgb) {
        return (0.299f * ((rgb >> 16) & 0xFF) + 0.587f * ((rgb >> 8) & 0xFF) + 0.114f * (rgb & 0xFF)) / 255f;
    }

    /**
     * What the hollow darkens towards. Snow scatters skylight into its shadows, so they go blue;
     * other ground keeps its own hue, darker. Dark ground has nothing to darken: there the hollow
     * holds a film of water and is drawn lighter, as a wet sheen.
     */
    static int shadowColour(int ground, Surface surface) {
        if (surface == Surface.SNOW) return 0x56688E;
        if (luminance(ground) < 0.30f) return scale(ground, 2.3f, 38);
        return scale(ground, 0.42f, 0);
    }

    /** The lip of pushed-up material catches more light than the flat ground around it. */
    static int rimColour(int ground) {
        if (luminance(ground) < 0.30f) return scale(ground, 0.7f, 0);
        return scale(ground, 1.12f, 20);
    }

    private static int scale(int rgb, float k, int add) {
        int r = clamp(((rgb >> 16) & 0xFF) * k + add), g = clamp(((rgb >> 8) & 0xFF) * k + add), b = clamp((rgb & 0xFF) * k + add);
        return (r << 16) | (g << 8) | b;
    }

    private static int clamp(float v) {
        return Math.max(0, Math.min(255, (int) v));
    }

    public void clear() {
        tracks.clear();
    }
}
