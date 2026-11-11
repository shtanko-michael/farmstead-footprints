package online.farmsteadminecraft.imprint.platform.forge;

import net.minecraftforge.fml.common.Mod;
import online.farmsteadminecraft.imprint.Imprint;
import online.farmsteadminecraft.imprint.ModLoaderAccess;

@Mod(Imprint.MOD_ID)
public class ImprintForge {
    public ImprintForge() {
        ModLoaderAccess.HOLDER.install(new ForgeLoaderAccess());
        Imprint.init();
        if (ModLoaderAccess.get().isClient()) {
            ForgeClientHooks.register();
        }
    }
}
