package online.farmsteadminecraft.footprints.platform.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import online.farmsteadminecraft.footprints.client.FootprintsClient;

/** Settings button in Mod Menu. Mod Menu is optional: without it this class is never loaded. */
public class FootprintsModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return FootprintsClient::configScreen;
    }
}
