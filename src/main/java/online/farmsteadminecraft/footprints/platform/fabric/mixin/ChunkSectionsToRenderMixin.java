package online.farmsteadminecraft.footprints.platform.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;

//? if >=1.21.6 && <26.1 {
/*import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import online.farmsteadminecraft.footprints.client.FootprintsClient;
import online.farmsteadminecraft.footprints.client.render.FootprintRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
*///?}

/**
 * 1.21.11 (Fabric): marks on water go right after the translucent terrain group, so they lie on the water instead of
 * cutting a hole in it. Listed in the mixin config only for that version; elsewhere this class is empty.
 */
@Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkSectionsToRender")
public abstract class ChunkSectionsToRenderMixin {
    //? if >=1.21.6 && <26.1 {
    /*@Inject(method = "renderGroup", at = @At("TAIL"))
    //? if >=1.21.11 {
    private void footprints$afterGroup(ChunkSectionLayerGroup group, com.mojang.blaze3d.textures.GpuSampler sampler, CallbackInfo ci) {
    //?} else
    /^private void footprints$afterGroup(ChunkSectionLayerGroup group, CallbackInfo ci) {^/
        if (group == ChunkSectionLayerGroup.TRANSLUCENT) {
            FootprintRenderer.renderImmediate(new Matrix4f(), FootprintsClient.cameraPos(Minecraft.getInstance()), FootprintRenderer.PASS_WATER);
        }
    }
    *///?}
}
