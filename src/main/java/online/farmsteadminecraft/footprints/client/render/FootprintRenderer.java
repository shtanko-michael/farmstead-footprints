package online.farmsteadminecraft.footprints.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import online.farmsteadminecraft.footprints.Footprints;
import online.farmsteadminecraft.footprints.client.Footprint;
import online.farmsteadminecraft.footprints.client.FootprintsClient;
import online.farmsteadminecraft.footprints.client.FootprintsConfig;
import org.joml.Matrix4f;

//? if >=26.1 {
/*import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.LightCoordsUtil;
*///?} else {
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
//?}

/**
 * Draws every live footprint as two translucent layers lying on the ground: first the hollow's
 * shadow, then the pushed-up lip around it. Neither layer is opaque, so the block texture shows
 * through and the print reads as pressed into the sand or snow rather than painted on it.
 * Each layer is one batch for all prints. Vertex positions are relative to the camera so they
 * stay precise far from the origin.
 */
public final class FootprintRenderer {
    public static final ResourceLocation SHADOW = Footprints.id("textures/footprints_shadow.png");
    public static final ResourceLocation RIM = Footprints.id("textures/footprints_rim.png");
    private static final float LIFT = 0.012f;
    /** Quads drawn in the last frame; shown by debug output and test hooks. */
    public static int lastDrawn;
    public static int frames;

    private FootprintRenderer() {
    }

    public static RenderType renderType(ResourceLocation texture) {
        //? if >=26.1 {
        /*return RenderTypes.entityTranslucent(texture);
        *///?} else
        return RenderType.entityTranslucent(texture);
    }

    //? if <26.1 {
    /**
     * Pre-26 path: draw straight into the immediate buffer during the level render.
     * {@code view} is the camera rotation matrix: the event pose stack on 1.20.1, the separate
     * model-view matrix the loaders expose from 1.20.5 on (their pose stack is identity there).
     */
    public static void renderImmediate(Matrix4f view, Vec3 camera) {
        online.farmsteadminecraft.footprints.client.Bench.frame();
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || !FootprintsConfig.ENABLED.on() || FootprintsClient.POOL.alive() == 0) return;
        Matrix4f matrix = new Matrix4f(view);
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        RenderType shadow = renderType(SHADOW);
        emit(buffers.getBuffer(shadow), matrix, camera, level, false);
        buffers.endBatch(shadow);
        RenderType rim = renderType(RIM);
        emit(buffers.getBuffer(rim), matrix, camera, level, true);
        buffers.endBatch(rim);
    }
    //?} else {
    /*/^* 26+ path: hand our geometry to the frame's submit collector. ^/
    public static void submit(SubmitNodeCollector collector, PoseStack pose, Vec3 camera) {
        online.farmsteadminecraft.footprints.client.Bench.frame();
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !FootprintsConfig.ENABLED.on() || FootprintsClient.POOL.alive() == 0) return;
        Matrix4f matrix = new Matrix4f(pose.last().pose());
        collector.submitCustomGeometry(pose, renderType(SHADOW), (p, vc) -> emit(vc, matrix, camera, level, false));
        collector.submitCustomGeometry(pose, renderType(RIM), (p, vc) -> emit(vc, matrix, camera, level, true));
    }
    *///?}

    /** Prints that passed the distance, fade and view-cone tests this frame, with their alpha. */
    private static int[] visible = new int[256];
    private static float[] alphas = new float[256];
    private static int visibleCount;
    /** cos of the widest angle from the view direction a print may sit at and still be drawn (80°). */
    private static final float CONE_COS = 0.17f;

    /**
     * The shadow pass picks the prints once (in reach, not faded out, not behind the camera) and refreshes
     * stale light values; the rim pass reuses that list. Each print then costs four vertex writes per
     * layer: corners, texture cell and light were worked out when it was placed.
     */
    private static void emit(VertexConsumer vc, Matrix4f matrix, Vec3 camera, ClientLevel level, boolean rim) {
        long t0 = online.farmsteadminecraft.footprints.client.Bench.now();
        Footprint[] prints = FootprintsClient.POOL.array();
        if (!rim) {
            frames++;
            collect(prints, camera, level);
        }
        float opacity = rim ? 0.85f : 1f;
        float lift = LIFT + (rim ? 0.0002f : 0f);
        for (int n = 0; n < visibleCount; n++) {
            Footprint p = prints[visible[n]];
            float cx = (float) (p.x - camera.x), cz = (float) (p.z - camera.z);
            // a tiny per-print lift keeps overlapping prints from z-fighting each other
            float y = (float) (p.y - camera.y) + lift + (n & 15) * 0.0004f;
            int rgb = rim ? p.rimRgb : p.shadowRgb;
            int a = Math.min(255, (int) (alphas[n] * opacity * 255));
            int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
            float[] c = p.corners;
            vertex(vc, matrix, cx + c[0], y, cz + c[1], p.u0, p.v0, r, g, b, a, p.light);
            vertex(vc, matrix, cx + c[2], y, cz + c[3], p.u0, p.v1, r, g, b, a, p.light);
            vertex(vc, matrix, cx + c[4], y, cz + c[5], p.u1, p.v1, r, g, b, a, p.light);
            vertex(vc, matrix, cx + c[6], y, cz + c[7], p.u1, p.v0, r, g, b, a, p.light);
        }
        if (!rim) lastDrawn = visibleCount;
        online.farmsteadminecraft.footprints.client.Bench.render(t0, rim ? 0 : visibleCount);
    }

    private static void collect(Footprint[] prints, Vec3 camera, ClientLevel level) {
        if (visible.length < prints.length) {
            visible = new int[prints.length];
            alphas = new float[prints.length];
        }
        long now = level.getGameTime();
        double maxSq = (double) FootprintsConfig.RENDER_DISTANCE.value() * FootprintsConfig.RENDER_DISTANCE.value();
        float opacity = FootprintsConfig.OPACITY.floatValue();
        org.joml.Vector3f look = FootprintsClient.lookVector(Minecraft.getInstance());
        int count = 0;
        for (int i = 0; i < prints.length; i++) {
            Footprint p = prints[i];
            if (!p.alive) continue;
            double dx = p.x - camera.x, dy = p.y - camera.y, dz = p.z - camera.z;
            double distSq = dx * dx + dz * dz;
            if (distSq > maxSq) continue;
            // behind the camera or far out to the side: off screen whatever the field of view
            double dot = dx * look.x() + dy * look.y() + dz * look.z();
            double len2 = distSq + dy * dy;
            if (len2 > 9.0 && (dot < 0 || dot * dot < CONE_COS * CONE_COS * len2)) continue;
            float alpha = p.depth * p.fade(now) * opacity;
            if (alpha <= 0.01f) continue;
            if (p.lightStale(now)) {
                p.light = light(level, p);
                p.lightTick = now;
            }
            visible[count] = i;
            alphas[count] = alpha;
            count++;
        }
        visibleCount = count;
    }

    private static int light(ClientLevel level, Footprint p) {
        BlockPos pos = BlockPos.containing(p.x, p.y + 0.1, p.z);
        //? if >=26.1 {
        /*return LightCoordsUtil.getLightCoords(level, pos);
        *///?} else
        return LevelRenderer.getLightColor(level, pos);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, float x, float y, float z, float u, float v,
                               int r, int g, int b, int a, int light) {
        //? if <1.21 {
        /*vc.vertex(m, x, y, z).color(r, g, b, a).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0f, 1f, 0f).endVertex();
        *///?} else
        vc.addVertex(m, x, y, z).setColor(r, g, b, a).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0f, 1f, 0f);
    }
}
