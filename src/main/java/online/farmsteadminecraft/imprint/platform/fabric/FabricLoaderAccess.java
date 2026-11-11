package online.farmsteadminecraft.imprint.platform.fabric;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import online.farmsteadminecraft.imprint.ModLoaderAccess;

final class FabricLoaderAccess implements ModLoaderAccess {
    private final FabricLoader loader = FabricLoader.getInstance();

    @Override
    public boolean isClient() {
        return loader.getEnvironmentType() == EnvType.CLIENT;
    }

    @Override
    public boolean isServer() {
        return loader.getEnvironmentType() == EnvType.SERVER;
    }

    @Override
    public boolean isModLoaded(String id) {
        return loader.isModLoaded(id);
    }

    @Override
    public java.nio.file.Path configDir() {
        return loader.getConfigDir();
    }

    @Override
    public String loaderName() {
        return "fabric";
    }
}
