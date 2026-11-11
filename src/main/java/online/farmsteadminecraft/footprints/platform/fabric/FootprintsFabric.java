package online.farmsteadminecraft.footprints.platform.fabric;

import net.fabricmc.api.ModInitializer;
import online.farmsteadminecraft.footprints.Footprints;
import online.farmsteadminecraft.footprints.ModLoaderAccess;

public class FootprintsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ModLoaderAccess.HOLDER.install(new FabricLoaderAccess());
        Footprints.init();
    }
}
