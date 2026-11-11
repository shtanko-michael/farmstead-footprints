package online.farmsteadminecraft.footprints.platform.fabric.mixin;

import net.minecraft.client.renderer.LevelRenderer;
import online.farmsteadminecraft.footprints.client.render.FootprintRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >=26.1 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
*///?} else if >=1.21.11 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.state.LevelRenderState;
import online.farmsteadminecraft.footprints.client.FootprintsClient;
import org.joml.Matrix4f;
*///?} else if >=1.21 {
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import online.farmsteadminecraft.footprints.client.FootprintsClient;
import org.joml.Matrix4f;
//?} else {
/*import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import online.farmsteadminecraft.footprints.client.FootprintsClient;
import org.joml.Matrix4f;
*///?}

/**
 * Draws the prints right after the entities, the same point the NeoForge and Forge builds use
 * (Fabric without Fabric API: no render event to listen to).
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    //? if >=26.1 {
    /*@Inject(method = "submitFeatures", at = @At("TAIL"))
    private void footprints$submit(LevelRenderState state, SubmitNodeCollector collector, boolean renderOutline, CallbackInfo ci) {
        FootprintRenderer.submit(collector, new PoseStack(), state.cameraRenderState.pos);
    }
    *///?} else if >=1.21.11 {
    /*// after the entities and their batches, where NeoForge fires AfterEntities; the model-view stack has the camera
    @Inject(method = "renderBlockDestroyAnimation", at = @At("HEAD"))
    private void footprints$render(PoseStack pose, MultiBufferSource.BufferSource buffers, LevelRenderState state, CallbackInfo ci) {
        FootprintRenderer.renderImmediate(new Matrix4f(), state.cameraRenderState.pos, FootprintRenderer.PASS_GROUND);
    }
    *///?} else if >=1.21 {
    // the global model-view matrix carries the camera rotation here, so the pose is identity
    @Inject(method = "renderLevel", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/LevelRenderer;checkPoseStack(Lcom/mojang/blaze3d/vertex/PoseStack;)V",
        ordinal = 0, shift = At.Shift.AFTER))
    private void footprints$render(CallbackInfo ci) {
        FootprintRenderer.renderImmediate(new Matrix4f(), FootprintsClient.cameraPos(Minecraft.getInstance()), FootprintRenderer.PASS_GROUND);
    }

    // marks on water go after the water itself
    @Inject(method = "renderSectionLayer", at = @At("TAIL"))
    private void footprints$afterLayer(RenderType layer, double x, double y, double z, Matrix4f frustum, Matrix4f projection,
                                       CallbackInfo ci) {
        if (layer == RenderType.translucent()) {
            FootprintRenderer.renderImmediate(new Matrix4f(), FootprintsClient.cameraPos(Minecraft.getInstance()), FootprintRenderer.PASS_WATER);
        }
    }
    //?} else {
    /*// 1.20.1: the level pose stack carries the camera rotation
    @Inject(method = "renderLevel", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/LevelRenderer;checkPoseStack(Lcom/mojang/blaze3d/vertex/PoseStack;)V",
        ordinal = 0, shift = At.Shift.AFTER))
    private void footprints$render(PoseStack poseStack, float partialTick, long finishNanoTime, boolean renderBlockOutline,
                                   Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projection,
                                   CallbackInfo ci) {
        FootprintRenderer.renderImmediate(poseStack.last().pose(), camera.getPosition(), FootprintRenderer.PASS_GROUND);
    }

    @Inject(method = "renderChunkLayer", at = @At("TAIL"))
    private void footprints$afterLayer(net.minecraft.client.renderer.RenderType layer, PoseStack poseStack, double x, double y, double z,
                                       Matrix4f projection, CallbackInfo ci) {
        if (layer == net.minecraft.client.renderer.RenderType.translucent()) {
            FootprintRenderer.renderImmediate(poseStack.last().pose(), FootprintsClient.cameraPos(net.minecraft.client.Minecraft.getInstance()),
                FootprintRenderer.PASS_WATER);
        }
    }
    *///?}
}
