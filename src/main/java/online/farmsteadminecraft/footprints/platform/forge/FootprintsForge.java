package online.farmsteadminecraft.footprints.platform.forge;

import net.minecraftforge.fml.common.Mod;
import online.farmsteadminecraft.footprints.Footprints;
import online.farmsteadminecraft.footprints.ModLoaderAccess;

@Mod(Footprints.MOD_ID)
public class FootprintsForge {
    public FootprintsForge() {
        ModLoaderAccess.HOLDER.install(new ForgeLoaderAccess());
        Footprints.init();
        if (ModLoaderAccess.get().isClient()) {
            ForgeClientHooks.register();
        }
    }
}
