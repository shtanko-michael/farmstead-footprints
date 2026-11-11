package online.farmsteadminecraft.imprint;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Imprint {
    public static final String MOD_ID = "imprint";
    public static final Logger LOGGER = LoggerFactory.getLogger("Imprint");
    public static final String VERSION = /*$ mod_version*/"0.1.0";
    public static final String MINECRAFT = /*$ minecraft*/"1.21.1";

    private Imprint() {
    }

    public static void init() {
        LOGGER.info("{} {} on {} {}", MOD_ID, VERSION, ModLoaderAccess.get().loaderName(), MINECRAFT);
    }

    public static ResourceLocation id(String path) {
        //? if <1.21 {
        /*return new ResourceLocation(MOD_ID, path);
         *///?} else
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
