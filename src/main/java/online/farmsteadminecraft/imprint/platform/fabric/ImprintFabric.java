package online.farmsteadminecraft.imprint.platform.fabric;

import net.fabricmc.api.ModInitializer;
import online.farmsteadminecraft.imprint.Imprint;
import online.farmsteadminecraft.imprint.ModLoaderAccess;

public class ImprintFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ModLoaderAccess.HOLDER.install(new FabricLoaderAccess());
        Imprint.init();
    }
}
