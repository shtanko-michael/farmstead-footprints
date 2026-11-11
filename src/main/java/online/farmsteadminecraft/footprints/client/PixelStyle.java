package online.farmsteadminecraft.footprints.client;

import java.util.List;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

//? if >=26.1 {
/*import java.util.ArrayList;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
*///?} else if >=1.21.11 {
/*import java.util.ArrayList;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.BakedQuad;
*///?} else {
import net.minecraft.client.renderer.block.model.BakedQuad;
//?}

/**
 * The pixel style: a print made of the ground's own texture pixels, pressed in.
 *
 * <p>At placement the print remembers how the block's top face maps world position to the block texture (the real
 * quad of the real model variant at that spot, so rotated grass tops line up) and the biome tint. Its shape is then
 * rasterised onto the world's 1/16-block texel grid: every texel the print covers is drawn again, darker, from the
 * same texture pixel, with the wall on the light side (north-west) in shadow and the far wall catching light, like a
 * dent in a vanilla texture. Runs of equal texels in a row become one quad, so a print costs a handful of quads.
 * Decay works on the same texels: the worn stages drop texels in the order the atlas erosion ranks them.</p>
 */
public final class PixelStyle {
    /** Shade per texel kind: 1 floor of the dent, 2 shadowed wall, 3 lit wall. */
    private static final float[] SHADE = {1f, 0.70f, 0.54f, 0.86f};
    private static final int MAX_SPAN = 40;
    private static final boolean[] in = new boolean[MAX_SPAN * MAX_SPAN];
    private static final byte[] kind = new byte[MAX_SPAN * MAX_SPAN];
    private static final RandomSource RANDOM = RandomSource.create();

    private PixelStyle() {
    }

    /** Reads the texture mapping and tint of the top face under the print. False when there is none (no model quad). */
    public static boolean capture(Footprint p, ClientLevel level, BlockPos pos, BlockState state) {
        Minecraft mc = Minecraft.getInstance();
        RANDOM.setSeed(state.getSeed(pos));
        float[] xs = new float[4], zs = new float[4], us = new float[4], vs = new float[4];
        int tintIndex;
        //? if >=26.1 {
        /*BlockStateModel model = mc.getModelManager().getBlockStateModelSet().get(state);
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(RANDOM, parts);
        BakedQuad quad = null;
        for (BlockStateModelPart part : parts) {
            List<BakedQuad> quads = part.getQuads(Direction.UP);
            if (!quads.isEmpty()) {
                quad = quads.get(quads.size() - 1);
                break;
            }
        }
        if (quad == null) return false;
        for (int i = 0; i < 4; i++) {
            xs[i] = quad.position(i).x();
            zs[i] = quad.position(i).z();
            us[i] = UVPair.unpackU(quad.packedUV(i));
            vs[i] = UVPair.unpackV(quad.packedUV(i));
        }
        tintIndex = quad.materialInfo().tintIndex();
        *///?} else if >=1.21.11 {
        /*BlockStateModel model = mc.getBlockRenderer().getBlockModel(state);
        List<BlockModelPart> parts = new ArrayList<>();
        model.collectParts(RANDOM, parts);
        BakedQuad quad = null;
        for (BlockModelPart part : parts) {
            List<BakedQuad> quads = part.getQuads(Direction.UP);
            if (!quads.isEmpty()) {
                quad = quads.get(quads.size() - 1);
                break;
            }
        }
        if (quad == null) return false;
        for (int i = 0; i < 4; i++) {
            xs[i] = quad.position(i).x();
            zs[i] = quad.position(i).z();
            us[i] = UVPair.unpackU(quad.packedUV(i));
            vs[i] = UVPair.unpackV(quad.packedUV(i));
        }
        tintIndex = quad.tintIndex();
        *///?} else {
        List<BakedQuad> quads = mc.getBlockRenderer().getBlockModel(state).getQuads(state, Direction.UP, RANDOM);
        if (quads.isEmpty()) return false;
        // the last top quad is the one on top (grass: the tinted overlay layer is drawn after the base)
        BakedQuad quad = quads.get(quads.size() - 1);
        int[] data = quad.getVertices();
        int stride = data.length / 4;
        for (int i = 0; i < 4; i++) {
            xs[i] = Float.intBitsToFloat(data[i * stride]);
            zs[i] = Float.intBitsToFloat(data[i * stride + 2]);
            us[i] = Float.intBitsToFloat(data[i * stride + 4]);
            vs[i] = Float.intBitsToFloat(data[i * stride + 5]);
        }
        tintIndex = quad.getTintIndex();
        //?}
        if (!affine(xs, zs, us, p.uMap) || !affine(xs, zs, vs, p.vMap)) return false;
        int tint = -1;
        if (tintIndex >= 0) {
            //? if >=26.1 {
            /*BlockTintSource source = mc.getBlockColors().getTintSource(state, tintIndex);
            if (source != null) tint = source.colorInWorld(state, level, pos);
            *///?} else
            tint = mc.getBlockColors().getColor(state, level, pos, tintIndex);
        }
        p.tint = tint == -1 ? 0xFFFFFF : tint & 0xFFFFFF;
        p.stripStage = -1;
        return true;
    }

    /** Fits value = a + b*x + c*z through the quad's corners (any three that are not in a line). */
    private static boolean affine(float[] xs, float[] zs, float[] val, float[] out) {
        for (int k = 0; k < 4; k++) {
            int i = k, j = (k + 1) % 4, l = (k + 2) % 4;
            float det = (xs[j] - xs[i]) * (zs[l] - zs[i]) - (xs[l] - xs[i]) * (zs[j] - zs[i]);
            if (Math.abs(det) < 1e-6f) continue;
            float b = ((val[j] - val[i]) * (zs[l] - zs[i]) - (val[l] - val[i]) * (zs[j] - zs[i])) / det;
            float c = ((xs[j] - xs[i]) * (val[l] - val[i]) - (xs[l] - xs[i]) * (val[j] - val[i])) / det;
            out[0] = val[i] - b * xs[i] - c * zs[i];
            out[1] = b;
            out[2] = c;
            return true;
        }
        return false;
    }

    /** Rasterises the print for one decay stage into {@link Footprint#strips}. */
    public static void rasterize(Footprint p, int stage, int stages) {
        p.stripStage = stage;
        p.stripCount = 0;
        double rad = Math.toRadians(p.yaw);
        float fx = (float) -Math.sin(rad), fz = (float) Math.cos(rad);
        float rx = (float) -Math.cos(rad), rz = (float) -Math.sin(rad);
        float h = p.half, reach = h * 1.4143f;
        int tx0 = (int) Math.floor((p.x - reach) * 16), tz0 = (int) Math.floor((p.z - reach) * 16);
        int w = Math.min(MAX_SPAN, (int) Math.floor((p.x + reach) * 16) - tx0 + 1);
        int d = Math.min(MAX_SPAN, (int) Math.floor((p.z + reach) * 16) - tz0 + 1);
        float threshold = stage / (float) stages;
        // half a texel in the print's texture space
        float tu = 1f / 32f / (2 * h);
        int cell = p.shape.cell;
        for (int j = 0; j < d; j++) {
            for (int i = 0; i < w; i++) {
                float wx = (float) ((tx0 + i + 0.5) / 16.0 - p.x), wz = (float) ((tz0 + j + 0.5) / 16.0 - p.z);
                float uf = 0.5f + (wx * rx + wz * rz) / (2 * h);
                float vf = 0.5f - (wx * fx + wz * fz) / (2 * h);
                // the texel's whole footprint, not just its centre: tread gaps and thin toes would break the shape up
                boolean hit = uf > -tu && uf < 1f + tu && vf > -tu && vf < 1f + tu
                    && coverage(cell, uf, vf, tu) > 0.38f
                    && (stage == 0 || FootprintAtlas.order(cell, clamp01(uf), clamp01(vf)) > threshold);
                in[j * MAX_SPAN + i] = hit;
            }
        }
        // light from the north-west, as in vanilla textures: that wall of the dent is in shadow, the far one lit
        for (int j = 0; j < d; j++) {
            for (int i = 0; i < w; i++) {
                int k = j * MAX_SPAN + i;
                if (!in[k]) {
                    kind[k] = 0;
                    continue;
                }
                boolean west = i == 0 || !in[k - 1], north = j == 0 || !in[k - MAX_SPAN];
                boolean east = i == w - 1 || !in[k + 1], south = j == d - 1 || !in[k + MAX_SPAN];
                kind[k] = (byte) (west || north ? 2 : east || south ? 3 : 1);
            }
        }
        int[] strips = p.strips;
        int n = 0;
        for (int j = 0; j < d; j++) {
            int i = 0;
            while (i < w) {
                int k = kind[j * MAX_SPAN + i];
                if (k == 0) {
                    i++;
                    continue;
                }
                int start = i;
                i++;
                // a run stops at another shade or at a block edge (the texture repeats per block)
                while (i < w && kind[j * MAX_SPAN + i] == k && Math.floorMod(tx0 + i, 16) != 0) i++;
                if (n == strips.length) strips = java.util.Arrays.copyOf(strips, n * 2);
                strips[n++] = start | (i - start) << 8 | j << 16 | k << 24;
            }
        }
        p.strips = strips;
        p.stripCount = n;
        p.texX = tx0;
        p.texZ = tz0;
    }

    /** Mean depth over a 3x3 grid across one texel (half-size {@code r} in cell space). */
    private static float coverage(int cell, float u, float v, float r) {
        float sum = 0;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                float su = u + dx * r * 0.67f, sv = v + dy * r * 0.67f;
                if (su >= 0f && su < 1f && sv >= 0f && sv < 1f) sum += FootprintAtlas.mask(cell, su, sv);
            }
        }
        return sum / 9f;
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(0.999f, v));
    }

    /** Writes the visible prints' texel runs. */
    public static void emit(VertexConsumer vc, Matrix4f m, Vec3 camera, Footprint[] prints, int[] visible, float[] alphas,
                            int[] stagesOf, int count, int stages, float opacity) {
        float lift = 0.0015f + FootprintsConfig.LIFT.floatValue();
        for (int n = 0; n < count; n++) {
            Footprint p = prints[visible[n]];
            if (!p.pixel) continue;
            if (p.stripStage != stagesOf[n]) rasterize(p, stagesOf[n], stages);
            float fade = Math.min(1f, alphas[n] / Math.max(0.01f, p.depth * opacity));
            int a = Math.min(255, (int) (fade * 255));
            float strength = Math.min(1f, p.depth * 1.15f) * opacity;
            int tr = (p.tint >> 16) & 0xFF, tg = (p.tint >> 8) & 0xFF, tb = p.tint & 0xFF;
            float y = (float) (p.y - camera.y) + lift + (n & 7) * 0.0002f;
            int light = p.light;
            for (int s = 0; s < p.stripCount; s++) {
                int strip = p.strips[s];
                int i0 = strip & 0xFF, len = (strip >> 8) & 0xFF, j = (strip >> 16) & 0xFF, k = (strip >> 24) & 0xFF;
                float shade = 1f - (1f - SHADE[k]) * strength;
                int r = (int) (tr * shade), g = (int) (tg * shade), b = (int) (tb * shade);
                int tx = p.texX + i0, tz = p.texZ + j;
                // block-local texel position, for the texture lookup
                float lx0 = Math.floorMod(tx, 16) / 16f, lx1 = lx0 + len / 16f;
                float lz0 = Math.floorMod(tz, 16) / 16f, lz1 = lz0 + 1 / 16f;
                float x0 = (float) (tx / 16.0 - camera.x), x1 = x0 + len / 16f;
                float z0 = (float) (tz / 16.0 - camera.z), z1 = z0 + 1 / 16f;
                vertex(vc, m, x0, y, z0, p, lx0, lz0, r, g, b, a, light);
                vertex(vc, m, x0, y, z1, p, lx0, lz1, r, g, b, a, light);
                vertex(vc, m, x1, y, z1, p, lx1, lz1, r, g, b, a, light);
                vertex(vc, m, x1, y, z0, p, lx1, lz0, r, g, b, a, light);
            }
        }
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, float x, float y, float z, Footprint p, float lx, float lz,
                               int r, int g, int b, int a, int light) {
        float u = p.uMap[0] + p.uMap[1] * lx + p.uMap[2] * lz;
        float v = p.vMap[0] + p.vMap[1] * lx + p.vMap[2] * lz;
        //? if <1.21 {
        /*vc.vertex(m, x, y, z).color(r, g, b, a).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0f, 1f, 0f).endVertex();
        *///?} else
        vc.addVertex(m, x, y, z).setColor(r, g, b, a).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0f, 1f, 0f);
    }
}
