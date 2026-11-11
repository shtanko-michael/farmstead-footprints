package online.farmsteadminecraft.footprints.platform.fabric;

import net.fabricmc.api.ClientModInitializer;
import online.farmsteadminecraft.footprints.client.FootprintsClient;

/**
 * Fabric client entry. No Fabric API: the tick, the drawing, the command and the translations are hooked
 * in by the mixins in {@code platform.fabric.mixin} (farmstead_footprints.fabric.mixins.json).
 */
public class FootprintsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FootprintsClient.init();
    }
}
