package online.farmsteadminecraft.imprint.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import online.farmsteadminecraft.imprint.Imprint;
import online.farmsteadminecraft.imprint.client.FootShape;
import online.farmsteadminecraft.imprint.client.Footprint;
import online.farmsteadminecraft.imprint.client.ImprintClient;
import online.farmsteadminecraft.imprint.client.ImprintConfig;
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
    public static final ResourceLocation SHADOW = Imprint.id("textures/footprints_shadow.png");
    public static final ResourceLocation RIM = Imprint.id("textures/footprints_rim.png");
    /** The print takes this share of its texture cell; the rest is room for the lip. */
    private static final float CELL_FILL = 0.80f;
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
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || !ImprintConfig.enabled || ImprintClient.POOL.alive() == 0) return;
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
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !ImprintConfig.enabled || ImprintClient.POOL.alive() == 0) return;
        Matrix4f matrix = new Matrix4f(pose.last().pose());
        collector.submitCustomGeometry(pose, renderType(SHADOW), (p, vc) -> emit(vc, matrix, camera, level, false));
        collector.submitCustomGeometry(pose, renderType(RIM), (p, vc) -> emit(vc, matrix, camera, level, true));
    }
    *///?}

    private static void emit(VertexConsumer vc, Matrix4f matrix, Vec3 camera, ClientLevel level, boolean rim) {
        long now = level.getGameTime();
        double maxSq = ImprintConfig.renderDistance * ImprintConfig.renderDistance;
        int[] order = {0};
        if (!rim) frames++;
        ImprintClient.POOL.forEachAlive(p -> {
            double dx = p.x - camera.x, dz = p.z - camera.z;
            if (dx * dx + dz * dz > maxSq) return;
            float alpha = p.depth * p.fade(now) * ImprintConfig.opacity;
            if (alpha <= 0.01f) return;
            // A tiny per-print lift keeps overlapping prints from z-fighting each other
            float y = (float) (p.y - camera.y) + LIFT + (order[0]++ % 16) * 0.0004f + (rim ? 0.0002f : 0f);
            quad(vc, matrix, p, (float) dx, y, (float) dz, rim ? alpha * 0.85f : alpha,
                rim ? p.rimRgb : p.shadowRgb, light(level, p));
        });
        if (!rim) lastDrawn = order[0];
    }

    private static int light(ClientLevel level, Footprint p) {
        BlockPos pos = BlockPos.containing(p.x, p.y + 0.1, p.z);
        //? if >=26.1 {
        /*return LightCoordsUtil.getLightCoords(level, pos);
        *///?} else
        return LevelRenderer.getLightColor(level, pos);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, Footprint p, float cx, float y, float cz, float alpha,
                             int rgb, int light) {
        FootShape s = p.shape;
        double rad = Math.toRadians(p.yaw);
        float fx = (float) -Math.sin(rad), fz = (float) Math.cos(rad);
        float rx = (float) -Math.cos(rad), rz = (float) -Math.sin(rad);
        // texture cells are square, the print fills CELL_FILL of the longer side
        float half = Math.max(s.width, s.length) * p.scale * 0.5f / CELL_FILL;
        float hl = half, hw = half;
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF, a = Math.min(255, (int) (alpha * 255));
        // toe-left, heel-left, heel-right, toe-right (counter-clockwise seen from above)
        vertex(vc, m, cx + fx * hl - rx * hw, y, cz + fz * hl - rz * hw, s.u0(), s.v0(), r, g, b, a, light);
        vertex(vc, m, cx - fx * hl - rx * hw, y, cz - fz * hl - rz * hw, s.u0(), s.v1(), r, g, b, a, light);
        vertex(vc, m, cx - fx * hl + rx * hw, y, cz - fz * hl + rz * hw, s.u1(), s.v1(), r, g, b, a, light);
        vertex(vc, m, cx + fx * hl + rx * hw, y, cz + fz * hl + rz * hw, s.u1(), s.v0(), r, g, b, a, light);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, float x, float y, float z, float u, float v,
                               int r, int g, int b, int a, int light) {
        //? if <1.21 {
        /*vc.vertex(m, x, y, z).color(r, g, b, a).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0f, 1f, 0f).endVertex();
        *///?} else
        vc.addVertex(m, x, y, z).setColor(r, g, b, a).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0f, 1f, 0f);
    }
}
