package online.farmsteadminecraft.footprints.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

/** Client-side state shared by all loaders: the print pool and the step tracker. */
public final class FootprintsClient {
    public static final FootprintPool POOL = new FootprintPool(2048);
    private static final StepTracker TRACKER = new StepTracker(POOL);
    private static ClientLevel lastLevel;
    private static boolean openConfig;

    private FootprintsClient() {
    }

    /** Loads the settings file; every loader calls this once when the client starts. */
    public static void init() {
        FootprintsConfig.load();
        if (FootprintsConfig.CHECK_UPDATES.on()) online.farmsteadminecraft.footprints.Footprints.checkForUpdate();
    }

    /** The settings screen, for the mod list (NeoForge/Forge) and Mod Menu (Fabric). */
    public static net.minecraft.client.gui.screens.Screen configScreen(net.minecraft.client.gui.screens.Screen parent) {
        return new online.farmsteadminecraft.footprints.config.ConfigScreen(parent, FootprintsConfig.FILE,
            FootprintsConfig::save, FootprintsConfig::load);
    }

    /** {@code /footprints config}: the chat screen closes after the command, so open on the next tick. */
    public static void openConfigNextTick() {
        openConfig = true;
    }

    public static void tick(Minecraft mc) {
        TestHooks.tick(mc);
        if (openConfig) {
            openConfig = false;
            //? if >=26.1 {
            /*mc.gui.setScreen(configScreen(null));
            *///?} else
            mc.setScreen(configScreen(null));
        }
        ClientLevel level = mc.level;
        if (level != lastLevel) {
            POOL.clear();
            TRACKER.clear();
            lastLevel = level;
        }
        if (level == null || !FootprintsConfig.ENABLED.on() || mc.isPaused()) return;
        long now = level.getGameTime();
        TRACKER.tick(level, cameraPos(mc), now);
        POOL.tick(now);
    }

    public static Vec3 cameraPos(Minecraft mc) {
        //? if >=26.1 {
        /*return mc.gameRenderer.mainCamera().position();
        *///?} else
        return mc.gameRenderer.getMainCamera().getPosition();
    }
}
