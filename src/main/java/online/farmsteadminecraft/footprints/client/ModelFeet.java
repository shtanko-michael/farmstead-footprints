package online.farmsteadminecraft.footprints.client;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;

/**
 * Feet of a mob we have no profile for, read from its model: the cubes that reach the lowest point of the model are
 * the feet, grouped into one foot per cluster. Two feet make a biped, more make a four-, six- or eight-legged walker
 * with those exact positions. Works for any mob whose renderer uses vanilla model parts (most mods; Blockbench's Java
 * export); others (GeckoLib and the like) fall back to the hitbox guess.
 */
final class ModelFeet {
    /** How close to the lowest point a cube must reach to count as standing on the ground, in blocks (1.5 model px). */
    private static final float GROUND_BAND = 0.094f;

    /** Part names that are legs, and names that touch the ground without being feet. */
    private static final java.util.regex.Pattern LIMB = java.util.regex.Pattern.compile("leg|foot|feet|paw|hoof|claw|toe");
    private static final java.util.regex.Pattern NOT_FOOT = java.util.regex.Pattern.compile("head|nose|beak|tail|jaw|snout|neck|mouth|horn|ear|wing");

    private ModelFeet() {
    }

    /** A profile from the entity's model, or null when the model cannot be read or shows no feet. */
    static FootProfile of(LivingEntity e) {
        try {
            List<float[]> cubes = cubes(e);
            if (cubes == null || cubes.isEmpty()) return null;
            return profile(e, cubes);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Every cube of the model: {bottom y, min x, max x, touch-down x, touch-down z, kind (1 limb, -1 not a foot, 0 ?)}
     * in blocks, model space (y down, -z forward).
     */
    private static List<float[]> cubes(LivingEntity e) {
        Object model = model(e);
        if (model == null) return null;
        List<ModelPart> roots = roots(model);
        if (roots.isEmpty()) return null;
        List<float[]> out = new ArrayList<>();
        Vector3f v = new Vector3f();
        for (ModelPart root : roots) {
            root.visit(new PoseStack(), (pose, path, index, cube) -> {
                float[] xs = new float[8], ys = new float[8], zs = new float[8];
                float bottom = -Float.MAX_VALUE, x0 = Float.MAX_VALUE, x1 = -Float.MAX_VALUE;
                for (int k = 0; k < 8; k++) {
                    v.set((k & 1) == 0 ? cube.minX : cube.maxX, (k & 2) == 0 ? cube.minY : cube.maxY, (k & 4) == 0 ? cube.minZ : cube.maxZ)
                        .mul(1f / 16f);
                    pose.pose().transformPosition(v);
                    xs[k] = v.x;
                    ys[k] = v.y;
                    zs[k] = v.z;
                    bottom = Math.max(bottom, v.y);
                    x0 = Math.min(x0, v.x);
                    x1 = Math.max(x1, v.x);
                }
                // where the cube touches down: the middle of its lowest corners (a slanted spider leg: its tip)
                float fx = 0, fz = 0;
                int n = 0;
                for (int k = 0; k < 8; k++) {
                    if (ys[k] >= bottom - 0.02f) {
                        fx += xs[k];
                        fz += zs[k];
                        n++;
                    }
                }
                String name = path.toLowerCase(java.util.Locale.ROOT);
                float kind = LIMB.matcher(name).find() ? 1f : NOT_FOOT.matcher(name).find() ? -1f : 0f;
                out.add(new float[]{bottom, x0, x1, fx / n, fz / n, kind});
            });
        }
        return out;
    }

    //? if >=1.21.2 {
    /*private static Object model(LivingEntity e) {
        EntityRenderer<?, ?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(e);
        return renderer instanceof LivingEntityRenderer<?, ?, ?> living ? living.getModel() : null;
    }

    private static List<ModelPart> roots(Object model) {
        return model instanceof net.minecraft.client.model.Model m ? List.of(m.root()) : List.of();
    }
    *///?} else {
    private static Object model(LivingEntity e) {
        EntityRenderer<?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(e);
        return renderer instanceof LivingEntityRenderer<?, ?> living ? living.getModel() : null;
    }

    private static List<ModelPart> roots(Object model) {
        if (model instanceof net.minecraft.client.model.HierarchicalModel<?> h) return List.of(h.root());
        // older-style models keep their parts in fields; visit only the top ones (a child visited on its own
        // would miss its parent's offset)
        List<ModelPart> parts = fieldParts(model);
        List<ModelPart> top = new ArrayList<>(parts);
        for (ModelPart p : parts) {
            p.getAllParts().forEach(child -> {
                if (child != p) top.remove(child);
            });
        }
        return top;
    }
    //?}

    private static FootProfile profile(LivingEntity e, List<float[]> cubes) {
        float lowest = -Float.MAX_VALUE, minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        for (float[] c : cubes) {
            lowest = Math.max(lowest, c[0]);
            minX = Math.min(minX, c[1]);
            maxX = Math.max(maxX, c[2]);
        }
        float modelWidth = Math.max(0.05f, maxX - minX);
        // feet: cubes reaching the ground band, not much wider than a leg (a body lying on the ground is not a foot);
        // when the model names its legs, only those
        boolean named = false;
        for (float[] c : cubes) if (c[5] > 0 && c[0] >= lowest - GROUND_BAND * 2) named = true;
        if (named) {
            lowest = -Float.MAX_VALUE;
            for (float[] c : cubes) if (c[5] > 0) lowest = Math.max(lowest, c[0]);
        }
        List<float[]> feet = new ArrayList<>();
        for (float[] c : cubes) {
            if (c[0] < lowest - GROUND_BAND || c[5] < 0 || named && c[5] <= 0) continue;
            if (c[2] - c[1] > modelWidth * 0.55f) continue;
            float cx = c[3], cz = c[4], size = Math.min(0.3f, c[2] - c[1]);
            boolean merged = false;
            for (float[] f : feet) {
                if (Math.abs(f[0] - cx) < 0.12f && Math.abs(f[1] - cz) < 0.12f) {
                    f[2] = Math.max(f[2], size);
                    merged = true;
                    break;
                }
            }
            if (!merged) feet.add(new float[]{cx, cz, size});
        }
        // all on the centre line: a tail or a belly, not legs
        boolean spread = false;
        for (float[] f : feet) if (Math.abs(f[0]) > 0.03f) spread = true;
        if (!spread) return null;
        if (feet.size() < 2 || feet.size() > 8) return null;
        // the model's size against the hitbox: renderers often scale models up or down
        // a model drawn much bigger or smaller than its hitbox was scaled by its renderer: follow the hitbox
        float ratio = e.getType().getWidth() / modelWidth;
        float k = ratio > 2.5f || ratio < 0.4f ? ratio : 1f;
        float footSize = 0f;
        for (float[] f : feet) footSize = Math.max(footSize, f[2]);
        if (feet.size() == 2) {
            float spacing = Math.abs(feet.get(0)[0] - feet.get(1)[0]) * 0.5f * k;
            float h = Math.max(0.3f, e.getType().getHeight());
            float stride = Math.max(0.25f, 0.92f * h / 1.95f);
            float scale = Math.max(0.3f, Math.min(2.5f, footSize * k / 0.25f));
            return FootProfile.biped(FootShape.BARE_LEFT, FootShape.BARE_RIGHT, stride, Math.max(0.05f, spacing), scale);
        }
        // one side's feet, front (most negative z) first, as model pixels
        boolean leftSide = feet.stream().filter(f -> f[0] > 0).count() >= feet.stream().filter(f -> f[0] < 0).count();
        List<float[]> side = new ArrayList<>();
        for (float[] f : feet) if ((f[0] > 0) == leftSide || Math.abs(f[0]) < 0.01f) side.add(f);
        if (side.size() < 2) return null;
        side.sort((a, b) -> Float.compare(a[1], b[1]));
        float[] px = new float[side.size() * 2];
        for (int i = 0; i < side.size(); i++) {
            px[i * 2] = Math.abs(side.get(i)[0]) * 16f;
            px[i * 2 + 1] = side.get(i)[1] * 16f;
        }
        float span = (side.get(side.size() - 1)[1] - side.get(0)[1]) * k;
        float stride = Math.max(0.25f, 0.7f * span);
        float scale = Math.max(0.3f, Math.min(2.5f, footSize * k / 0.25f));
        return FootProfile.quad(FootShape.PAW, scale, stride, 0f, 0f, k, px);
    }

    /** Model parts held in the model's fields, for models that have no single root (pre-26). */
    static List<ModelPart> fieldParts(Object model) {
        List<ModelPart> parts = new ArrayList<>();
        for (Class<?> c = model.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) || f.getType() != ModelPart.class) continue;
                try {
                    f.setAccessible(true);
                    Object p = f.get(model);
                    if (p instanceof ModelPart part && !parts.contains(part)) parts.add(part);
                } catch (Exception ignored) {
                    // a module system or security manager said no: skip this field
                }
            }
        }
        return parts;
    }
}
