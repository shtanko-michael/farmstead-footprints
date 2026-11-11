package online.farmsteadminecraft.imprint.platform.forge;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import online.farmsteadminecraft.imprint.ModLoaderAccess;

final class ForgeLoaderAccess implements ModLoaderAccess {
    @Override
    public boolean isClient() {
        return FMLEnvironment.dist.isClient();
    }

    @Override
    public boolean isServer() {
        return FMLEnvironment.dist.isDedicatedServer();
    }

    @Override
    public boolean isModLoaded(String id) {
        return ModList.get().isLoaded(id);
    }

    @Override
    public String loaderName() {
        return "forge";
    }
}
