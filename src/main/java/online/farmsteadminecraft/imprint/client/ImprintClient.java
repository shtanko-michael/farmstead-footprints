package online.farmsteadminecraft.imprint.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

/** Client-side state shared by all loaders: the print pool and the step tracker. */
public final class ImprintClient {
    public static final FootprintPool POOL = new FootprintPool(ImprintConfig.maxDecals);
    private static final StepTracker TRACKER = new StepTracker(POOL);
    private static ClientLevel lastLevel;

    private ImprintClient() {
    }

    public static void tick(Minecraft mc) {
        TestHooks.tick(mc);
        ClientLevel level = mc.level;
        if (level != lastLevel) {
            POOL.clear();
            TRACKER.clear();
            lastLevel = level;
        }
        if (level == null || !ImprintConfig.enabled || mc.isPaused()) return;
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
