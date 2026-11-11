package online.farmsteadminecraft.footprints;

import net.minecraft.resources.ResourceLocation;
import online.farmsteadminecraft.footprints.brand.FarmsteadBrand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Footprints {
    public static final String MOD_ID = "farmstead_footprints";
    public static final String NAME = "Footprints";
    /** Project slug on Modrinth: the start-up line links there and the update check asks it. */
    public static final String MODRINTH_SLUG = "footprints";
    public static final Logger LOGGER = LoggerFactory.getLogger("Footprints");
    public static final String VERSION = /*$ mod_version*/"0.1.0";
    public static final String MINECRAFT = /*$ minecraft*/"1.21.1";

    private Footprints() {
    }

    public static void init() {
        String loader = ModLoaderAccess.get().loaderName();
        FarmsteadBrand.started(LOGGER, NAME, VERSION, loader, MINECRAFT, MODRINTH_SLUG);
        // A client checks after its settings load (they can turn it off); a server has only the global switch
        if (ModLoaderAccess.get().isServer()) checkForUpdate();
    }

    public static void checkForUpdate() {
        FarmsteadBrand.checkForUpdate(LOGGER, NAME, VERSION, ModLoaderAccess.get().loaderName(), MINECRAFT, MODRINTH_SLUG);
    }

    public static ResourceLocation id(String path) {
        //? if <1.21 {
        /*return new ResourceLocation(MOD_ID, path);
         *///?} else
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
