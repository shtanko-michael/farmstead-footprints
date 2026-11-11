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

    public StepTracker(FootprintPool pool) {
        this.pool = pool;
    }

    private static final class Track {
        double lastX, lastZ;
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
            if (profile == null) continue;
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
            float stride = profile.stride() * pace * (profile.quadruped() ? scaleOf(living) : 1f);
            if (track.walked >= stride) {
                track.walked -= stride;
                if (track.walked > stride) track.walked = 0;
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

    /** Babies are smaller; on 1.21+ the scale attribute too. */
    private static float scaleOf(LivingEntity e) {
        return Math.max(0.2f, e.getScale());
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
            double side = sign * profile.spacing();
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
        double y = e.getY();
        // A thin cover (a snow layer) can sit in the block the feet are in; the print belongs on top of
        // it, not on the block underneath, or the cover hides it. Server-placed entities and camera-rig
        // actors often stand at the block's integer top with the layer around their feet.
        BlockPos pos = BlockPos.containing(x, y + 0.01, z);
        BlockState state = level.getBlockState(pos);
        Surface surface = Surfaces.of(state);
        if (surface == null) {
            pos = BlockPos.containing(x, y - 0.05, z);
            state = level.getBlockState(pos);
            surface = Surfaces.of(state);
        }
        if (surface == null) return;
        net.minecraft.world.phys.shapes.VoxelShape outline = state.getShape(level, pos);
        double top = outline.isEmpty() ? pos.getY() : pos.getY() + outline.max(net.minecraft.core.Direction.Axis.Y);
        Footprint p = pool.add();
        // no two prints alike: a few degrees, a few percent, a few millimetres
        p.x = x + (random.nextFloat() - 0.5f) * 0.06f;
        p.y = Math.max(top, Math.min(y, top + 0.2));
        p.z = z + (random.nextFloat() - 0.5f) * 0.06f;
        p.yaw = yaw + (random.nextFloat() - 0.5f) * 14f;
        p.shape = e.isShiftKeyDown() && !profile.quadruped() ? FootShape.SNEAK : shape;
        p.scale = profile.scale() * (0.92f + random.nextFloat() * 0.16f);
        p.depth = surface.depth() * (e.isShiftKeyDown() ? 0.55f : 1f) * (0.85f + random.nextFloat() * 0.2f)
            * (e.isSprinting() ? 1.1f : 1f);
        int ground = state.getMapColor(level, pos).col;
        p.shadowRgb = shadowColour(ground, surface);
        p.rimRgb = rimColour(ground);
        p.bornTick = now;
        p.lifeTicks = (int) Math.max(40, surface.lifeSecs() * 20 * FootprintsConfig.LIFETIME.value()
            * (0.9f + random.nextFloat() * 0.2f));
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
