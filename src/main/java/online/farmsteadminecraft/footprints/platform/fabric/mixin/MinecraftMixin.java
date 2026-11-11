package online.farmsteadminecraft.footprints.platform.fabric.mixin;

import net.minecraft.client.Minecraft;
import online.farmsteadminecraft.footprints.client.FootprintsClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** End of every client tick (Fabric without Fabric API: this is all the tick event did). */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void footprints$tick(CallbackInfo ci) {
        FootprintsClient.tick((Minecraft) (Object) this);
    }
}
