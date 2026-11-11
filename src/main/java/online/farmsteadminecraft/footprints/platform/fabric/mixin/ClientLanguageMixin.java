package online.farmsteadminecraft.footprints.platform.fabric.mixin;

import java.util.List;
import java.util.Map;

import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.server.packs.resources.Resource;
import online.farmsteadminecraft.footprints.platform.fabric.ModLang;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Without Fabric API nobody adds the mod's own files to the game's resources, so its translations are read
 * straight from the jar and added after the resource packs' (a resource pack can still override them).
 */
@Mixin(ClientLanguage.class)
public abstract class ClientLanguageMixin {
    @Inject(method = "appendFrom(Ljava/lang/String;Ljava/util/List;Ljava/util/Map;)V", at = @At("TAIL"))
    private static void footprints$lang(String language, List<Resource> resources, Map<String, String> out, CallbackInfo ci) {
        ModLang.addTo(language, out);
    }
}
