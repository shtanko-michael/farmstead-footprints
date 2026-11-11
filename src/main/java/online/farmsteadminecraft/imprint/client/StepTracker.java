package online.farmsteadminecraft.imprint.client;

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

    public StepTracker(FootprintPool pool) {
        this.pool = pool;
    }

    private static final class Track {
        double lastX, lastZ;
        double walked;
        boolean right;
        long seen;
        boolean valid;
    }

    public void tick(ClientLevel level, Vec3 camera, long now) {
        double maxDistSq = ImprintConfig.renderDistance * ImprintConfig.renderDistance;
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
                track.valid = true;
                continue;
            }
            double moved = Math.sqrt(Mth.square(x - track.lastX) + Mth.square(z - track.lastZ));
            track.lastX = x;
            track.lastZ = z;
            if (moved > TELEPORT_DISTANCE) {
                track.walked = 0;
                continue;
            }
            FootProfile profile = FootProfiles.of(living);
            if (profile == null) continue;
            track.walked += moved;
            float stride = profile.stride() * Math.max(0.5f, e.getBbWidth() / 0.6f * 0.6f + 0.4f);
            if (track.walked >= stride) {
                track.walked -= stride;
                if (track.walked > stride) track.walked = 0;
                step(level, living, profile, track.right, now);
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

    private void step(ClientLevel level, LivingEntity e, FootProfile profile, boolean right, long now) {
        float yaw = e.getYRot();
        if (!(e instanceof net.minecraft.world.entity.player.Player)) yaw = e.yBodyRot;
        double rad = Math.toRadians(yaw);
        // Minecraft yaw 0 faces +Z; right of that is -X
        double fx = -Math.sin(rad), fz = Math.cos(rad);
        double rx = -Math.cos(rad), rz = -Math.sin(rad);
        double side = (right ? 1 : -1) * profile.spacing() * Math.max(1f, e.getBbWidth() / 0.6f);
        if (profile.quadruped()) {
            double half = Math.max(0.25, e.getBbWidth() * 0.45);
            place(level, e, profile, right ? profile.right() : profile.left(), e.getX() + rx * side + fx * half, e.getZ() + rz * side + fz * half, yaw, now);
            place(level, e, profile, right ? profile.left() : profile.right(), e.getX() - rx * side - fx * half, e.getZ() - rz * side - fz * half, yaw, now);
        } else {
            place(level, e, profile, right ? profile.right() : profile.left(), e.getX() + rx * side, e.getZ() + rz * side, yaw, now);
        }
    }

    private void place(ClientLevel level, LivingEntity e, FootProfile profile, FootShape shape, double x, double z, float yaw, long now) {
        double y = e.getY();
        BlockPos pos = BlockPos.containing(x, y - 0.05, z);
        BlockState state = level.getBlockState(pos);
        Surface surface = Surfaces.of(state);
        if (surface == null) return;
        Footprint p = pool.add();
        p.x = x;
        p.y = y;
        p.z = z;
        p.yaw = yaw;
        p.shape = shape;
        p.scale = profile.scale();
        p.depth = surface.depth() * (e.isShiftKeyDown() ? 0.6f : 1f);
        p.rgb = tint(state.getMapColor(level, pos).col, surface.darken());
        p.bornTick = now;
        p.lifeTicks = (int) Math.max(40, surface.lifeSecs() * 20 * ImprintConfig.lifetimeMultiplier);
    }

    /**
     * Vertex tint for a print. The texture darkens the floor of the print by up to half and
     * keeps the rim at full value, so the tint is a little brighter than the ground: the rim
     * reads as pushed-up edge, the floor as shadow. Dark ground (mud, soul soil) would swallow
     * a shadow, so there the tint is lifted further and reads as a wet sheen instead.
     */
    private static int tint(int rgb, float darken) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        float lum = (0.299f * r + 0.587f * g + 0.114f * b) / 255f;
        float k = lum < 0.30f ? 2.1f : 1.0f - darken * 0.1f;
        return (clamp(r * k) << 16) | (clamp(g * k) << 8) | clamp(b * k);
    }

    private static int clamp(float v) {
        return Math.max(0, Math.min(255, (int) v));
    }

    public void clear() {
        tracks.clear();
    }
}
