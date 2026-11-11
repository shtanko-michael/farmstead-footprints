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
*///?} else if >=1.21 {
import net.minecraft.client.Minecraft;
import online.farmsteadminecraft.footprints.client.FootprintsClient;
import org.joml.Matrix4f;
//?} else {
/*import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
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
    *///?} else if >=1.21 {
    // the global model-view matrix carries the camera rotation here, so the pose is identity
    @Inject(method = "renderLevel", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/LevelRenderer;checkPoseStack(Lcom/mojang/blaze3d/vertex/PoseStack;)V",
        ordinal = 0, shift = At.Shift.AFTER))
    private void footprints$render(CallbackInfo ci) {
        FootprintRenderer.renderImmediate(new Matrix4f(), FootprintsClient.cameraPos(Minecraft.getInstance()));
    }
    //?} else {
    /*// 1.20.1: the level pose stack carries the camera rotation
    @Inject(method = "renderLevel", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/LevelRenderer;checkPoseStack(Lcom/mojang/blaze3d/vertex/PoseStack;)V",
        ordinal = 0, shift = At.Shift.AFTER))
    private void footprints$render(PoseStack poseStack, float partialTick, long finishNanoTime, boolean renderBlockOutline,
                                   Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projection,
                                   CallbackInfo ci) {
        FootprintRenderer.renderImmediate(poseStack.last().pose(), camera.getPosition());
    }
    *///?}
}
