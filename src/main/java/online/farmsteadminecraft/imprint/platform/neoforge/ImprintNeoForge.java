package online.farmsteadminecraft.imprint.platform.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import online.farmsteadminecraft.imprint.Imprint;
import online.farmsteadminecraft.imprint.ModLoaderAccess;

@Mod(Imprint.MOD_ID)
public class ImprintNeoForge {
    public ImprintNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        ModLoaderAccess.HOLDER.install(new NeoForgeLoaderAccess());
        Imprint.init();
        if (ModLoaderAccess.get().isClient()) {
            NeoForgeClientHooks.register();
        }
    }
}
