package online.farmsteadminecraft.footprints.platform.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import online.farmsteadminecraft.footprints.ModLoaderAccess;

final class NeoForgeLoaderAccess implements ModLoaderAccess {
    private static Dist dist() {
        //? if >=1.21.9 {
        /*return FMLEnvironment.getDist();
        *///?} else
        return FMLEnvironment.dist;
    }

    @Override
    public boolean isClient() {
        return dist().isClient();
    }

    @Override
    public boolean isServer() {
        return dist().isDedicatedServer();
    }

    @Override
    public boolean isModLoaded(String id) {
        return ModList.get().isLoaded(id);
    }

    @Override
    public java.nio.file.Path configDir() {
        return net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get();
    }

    @Override
    public String loaderName() {
        return "neoforge";
    }
}
