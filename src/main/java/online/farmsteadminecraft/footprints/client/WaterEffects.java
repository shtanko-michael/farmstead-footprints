package online.farmsteadminecraft.footprints.client;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
*///?} else {
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
//?}

/**
 * Everything that happens on and in water, and on rails: foam wakes behind boats and rings where something wades
 * (decals, drawn with the prints), spray, droplets from wet feet, bubbles behind swimmers, sparks under fast carts.
 * Also retires prints whose block was broken.
 */
public final class WaterEffects {
    private static final float FOAM_ALPHA = 0.42f;
    private static final int FOAM_RGB = 0xE6F2FF;

    private final FootprintPool pool;
    private final Int2ObjectMap<double[]> odometers = new Int2ObjectOpenHashMap<>();
    private final java.util.Random random = new java.util.Random();
    private int checkCursor;

    public WaterEffects(FootprintPool pool) {
        this.pool = pool;
    }

    public void clear() {
        odometers.clear();
    }

    public void tick(ClientLevel level, Vec3 camera, long now) {
        double maxDistSq = (double) FootprintsConfig.RENDER_DISTANCE.value() * FootprintsConfig.RENDER_DISTANCE.value();
        for (Entity e : level.entitiesForRendering()) {
            if (e.position().distanceToSqr(camera) > maxDistSq) continue;
            //? if >=1.21.11 {
            /*boolean boat = e instanceof AbstractBoat;
            *///?} else
            boolean boat = e instanceof Boat;
            if (boat) {
                boat(level, e, now);
            } else if (e instanceof AbstractMinecart) {
                cart(level, e);
            } else if (e instanceof LivingEntity living && !living.isSpectator()) {
                if (living.isSwimming() && FootprintsConfig.SWIM_BUBBLES.on() && now % 2 == 0) bubbles(level, living);
                else if (living.isInWater() && !living.isUnderWater() && FootprintsConfig.RIPPLES.on()) wade(level, living, now);
            }
        }
        if (now % 20 == 0) odometers.int2ObjectEntrySet().removeIf(en -> now - (long) en.getValue()[3] > 100);
        if (FootprintsConfig.REMOVE_ON_CHANGE.on()) retireBroken(level);
    }

    /** x, z, distance since the last mark, tick last seen. */
    private double[] odometer(Entity e, long now) {
        double[] o = odometers.computeIfAbsent(e.getId(), id -> new double[]{e.getX(), e.getZ(), 0, now});
        double moved = Math.sqrt(Mth.square(e.getX() - o[0]) + Mth.square(e.getZ() - o[1]));
        o[0] = e.getX();
        o[1] = e.getZ();
        o[2] += moved > 3 ? 0 : moved;
        o[3] = now;
        return o;
    }

    private void boat(ClientLevel level, Entity e, long now) {
        if (!FootprintsConfig.BOAT_WAKE.on()) return;
        double[] o = odometer(e, now);
        // from positions, not the motion vector: other players' boats and server-moved ones report no motion
        Vec3 v = motion(e);
        double speed = Math.sqrt(v.x * v.x + v.z * v.z);
        if (speed < 0.06) return;
        double surface = waterSurface(level, e.getX(), e.getY() + 0.3, e.getZ());
        if (Double.isNaN(surface)) return;
        float yaw = (float) Math.toDegrees(Math.atan2(-v.x, v.z));
        double fx = -Math.sin(Math.toRadians(yaw)), fz = Math.cos(Math.toRadians(yaw));
        if (o[2] >= 0.45) {
            o[2] = 0;
            // a slice at the stern, as wide as the boat; it spreads as it ages, faster boats spread it more
            float size = 1.1f;
            decal(level, e.getX() - fx * 0.9, surface, e.getZ() - fz * 0.9, yaw, FootShape.WAKE, size,
                FOAM_ALPHA * (float) Math.min(1.0, 0.4 + speed * 1.5), 140 + random.nextInt(30), now,
                (float) Math.min(8.0, 2.0 + speed * 14));
        }
        if (speed > 0.2) {
            // spray off both sides of the bow
            double rx = -Math.cos(Math.toRadians(yaw)), rz = -Math.sin(Math.toRadians(yaw));
            for (int side = -1; side <= 1; side += 2) {
                double px = e.getX() + fx * 0.8 + rx * side * 0.6, pz = e.getZ() + fz * 0.8 + rz * side * 0.6;
                level.addParticle(ParticleTypes.SPLASH, px, surface + 0.05, pz, rx * side * 0.1, 0.1 + speed * 0.2, rz * side * 0.1);
            }
        }
    }

    private void wade(ClientLevel level, LivingEntity e, long now) {
        double[] o = odometer(e, now);
        if (o[2] < 0.9) return;
        o[2] = 0;
        double surface = waterSurface(level, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ());
        if (Double.isNaN(surface)) return;
        float size = Math.max(0.6f, e.getBbWidth() * 1.8f);
        decal(level, e.getX(), surface, e.getZ(), random.nextFloat() * 360f, FootShape.RIPPLE, size * 0.6f, 0.4f, 50 + random.nextInt(15), now, 1.6f);
    }

    private static Vec3 motion(Entity e) {
        return new Vec3(e.getX() - e.xo, e.getY() - e.yo, e.getZ() - e.zo);
    }

    private void bubbles(ClientLevel level, LivingEntity e) {
        Vec3 v = motion(e);
        level.addParticle(ParticleTypes.BUBBLE, e.getX() - v.x * 2 + (random.nextDouble() - 0.5) * 0.3,
            e.getY() + e.getBbHeight() * 0.3, e.getZ() - v.z * 2 + (random.nextDouble() - 0.5) * 0.3, 0, 0.02, 0);
    }

    private void cart(ClientLevel level, Entity e) {
        if (!FootprintsConfig.RAIL_SPARKS.on()) return;
        Vec3 v = motion(e);
        double speed = Math.sqrt(v.x * v.x + v.z * v.z);
        if (speed < 0.3) return;
        BlockPos at = e.blockPosition();
        BlockState rail = level.getBlockState(at);
        if (!rail.is(net.minecraft.tags.BlockTags.RAILS)) return;
        float chance = (rail.is(Blocks.POWERED_RAIL) ? 0.8f : 0.4f) * (float) Math.min(1.0, speed * 1.5);
        if (random.nextFloat() > chance) return;
        double side = random.nextBoolean() ? 0.4 : -0.4;
        double rx = -v.z / speed, rz = v.x / speed;
        level.addParticle(ParticleTypes.ELECTRIC_SPARK, e.getX() + rx * side, at.getY() + 0.1, e.getZ() + rz * side,
            -v.x * 0.3 + (random.nextDouble() - 0.5) * 0.1, 0.05 + random.nextDouble() * 0.1, -v.z * 0.3 + (random.nextDouble() - 0.5) * 0.1);
    }

    /** Droplets from wet feet, scaled by how wet they still are. */
    static void splash(ClientLevel level, LivingEntity e, double x, double y, double z, float wet) {
        FootprintsConfig.Dust mode = FootprintsConfig.SPLASH.get();
        if (mode == FootprintsConfig.Dust.OFF || mode == FootprintsConfig.Dust.PLAYERS && !(e instanceof Player)) return;
        int n = 2 + (int) (wet * 4);
        for (int i = 0; i < n; i++) {
            level.addParticle(ParticleTypes.SPLASH, x + (Math.random() - 0.5) * 0.3, y + 0.05, z + (Math.random() - 0.5) * 0.3,
                (Math.random() - 0.5) * 0.1, 0.12, (Math.random() - 0.5) * 0.1);
        }
    }

    /** Height of the water's surface in the column at (x, z), searched down from y; NaN if there is no open water. */
    private static double waterSurface(ClientLevel level, double x, double y, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dy = 0; dy >= -2; dy--) {
            pos.set(x, y + dy, z);
            FluidState fluid = level.getFluidState(pos);
            if (!fluid.is(FluidTags.WATER)) continue;
            BlockPos above = pos.above();
            if (level.getFluidState(above).is(FluidTags.WATER)) {
                pos.set(above);
                continue;
            }
            if (!level.getBlockState(above).isAir()) return Double.NaN;
            return pos.getY() + fluid.getHeight(level, pos);
        }
        return Double.NaN;
    }

    private void decal(ClientLevel level, double x, double y, double z, float yaw, FootShape shape, float size, float alpha,
                       int life, long now, float grow) {
        //? if >=26.1 {
        /*// 26.x draws custom geometry before the water, so a decal there would cut a hole in it: foam particles instead
        double rx = -Math.cos(Math.toRadians(yaw)), rz = -Math.sin(Math.toRadians(yaw));
        if (shape == FootShape.WAKE) {
            for (int side = -1; side <= 1; side += 2) {
                double px = x + rx * side * size * 0.45, pz = z + rz * side * size * 0.45;
                level.addParticle(ParticleTypes.FISHING, px, y, pz, rx * side * 0.03, 0, rz * side * 0.03);
            }
            level.addParticle(ParticleTypes.BUBBLE_POP, x, y + 0.05, z, 0, 0.01, 0);
        } else {
            for (int k = 0; k < 4; k++) {
                double a = k * Math.PI / 2 + random.nextDouble();
                level.addParticle(ParticleTypes.FISHING, x + Math.cos(a) * size * 0.4, y, z + Math.sin(a) * size * 0.4,
                    Math.cos(a) * 0.02, 0, Math.sin(a) * 0.02);
            }
        }
        if (true) return;
        *///?}
        Footprint p = pool.add();
        p.x = x;
        p.y = y - 0.008;
        p.onWater = true;
        p.z = z;
        p.yaw = yaw;
        p.scale = size;
        p.shape = shape;
        p.shadowRgb = FOAM_RGB;
        p.rimRgb = FOAM_RGB;
        p.rim = 0f;
        p.depth = alpha;
        p.exposed = false;
        p.bornTick = now;
        p.lifeTicks = life;
        p.grow = grow;
        BlockPos pos = BlockPos.containing(x, y - 0.05, z);
        p.supportPos = pos.asLong();
        p.supportBlock = level.getBlockState(pos).getBlock();
        p.prepare();
        p.pixel = false;
    }

    /** Checks a slice of the prints each tick: a print whose block was broken or replaced goes with it. */
    private void retireBroken(ClientLevel level) {
        Footprint[] prints = pool.array();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int n = Math.min(prints.length, 256);
        for (int k = 0; k < n; k++) {
            checkCursor = (checkCursor + 1) % prints.length;
            Footprint p = prints[checkCursor];
            if (!p.alive || p.supportBlock == null) continue;
            pos.set(BlockPos.getX(p.supportPos), BlockPos.getY(p.supportPos), BlockPos.getZ(p.supportPos));
            if (!level.isLoaded(pos)) continue;
            if (level.getBlockState(pos).getBlock() != p.supportBlock) p.bornTick = Long.MIN_VALUE / 4;
        }
    }
}
