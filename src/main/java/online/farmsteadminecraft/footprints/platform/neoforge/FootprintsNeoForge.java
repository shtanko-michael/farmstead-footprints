package online.farmsteadminecraft.footprints.platform.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import online.farmsteadminecraft.footprints.Footprints;
import online.farmsteadminecraft.footprints.ModLoaderAccess;

@Mod(Footprints.MOD_ID)
public class FootprintsNeoForge {
    public FootprintsNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        ModLoaderAccess.HOLDER.install(new NeoForgeLoaderAccess());
        Footprints.init();
        if (ModLoaderAccess.get().isClient()) {
            NeoForgeClientHooks.register(modContainer);
        }
    }
}
