package online.farmsteadminecraft.imprint.platform.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import online.farmsteadminecraft.imprint.client.ImprintClient;

/** Settings button in Mod Menu. Mod Menu is optional: without it this class is never loaded. */
public class ImprintModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ImprintClient::configScreen;
    }
}
