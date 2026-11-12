package online.farmsteadminecraft.footprints.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

/** Client-side state shared by all loaders: the print pool and the step tracker. */
public final class FootprintsClient {
    public static final FootprintPool POOL = new FootprintPool(2048);
    private static final StepTracker TRACKER = new StepTracker(POOL);
    private static final WaterEffects WATER = new WaterEffects(POOL);
    private static ClientLevel lastLevel;
    private static boolean openConfig;

    private FootprintsClient() {
    }

    /** Loads the settings file; every loader calls this once when the client starts. */
    public static void init() {
        FootprintsConfig.load();
        if (FootprintsConfig.CHECK_UPDATES.on()) online.farmsteadminecraft.footprints.Footprints.checkForUpdate();
        else online.farmsteadminecraft.footprints.Footprints.updateCheckOff();
    }

    /** The settings screen, for the mod list (NeoForge/Forge) and Mod Menu (Fabric). */
    public static net.minecraft.client.gui.screens.Screen configScreen(net.minecraft.client.gui.screens.Screen parent) {
        String version = online.farmsteadminecraft.footprints.Footprints.VERSION;
        String repo = "https://github.com/shtanko-michael/farmstead-footprints";
        var extras = new online.farmsteadminecraft.footprints.config.ConfigScreen.Extras(
            online.farmsteadminecraft.footprints.Footprints.NAME, version,
            () -> online.farmsteadminecraft.footprints.brand.FarmsteadBrand.newerThan(version),
            online.farmsteadminecraft.footprints.brand.FarmsteadBrand::source,
            java.util.List.of(
                new online.farmsteadminecraft.footprints.config.ConfigScreen.Link("modrinth", "Modrinth page", "https://modrinth.com/mod/farmstead-footprints"),
                new online.farmsteadminecraft.footprints.config.ConfigScreen.Link("curseforge", "CurseForge page", "https://www.curseforge.com/minecraft/mc-mods/farmstead-footprints"),
                new online.farmsteadminecraft.footprints.config.ConfigScreen.Link("issues", "Report a bug or an idea", repo + "/issues"),
                new online.farmsteadminecraft.footprints.config.ConfigScreen.Link("wiki", "How to: own textures and pack rules", repo + "#your-own-prints")),
            FootprintsClient::tryIt);
        return new online.farmsteadminecraft.footprints.config.ConfigScreen(parent, FootprintsConfig.FILE,
            FootprintsConfig::save, FootprintsConfig::load, extras);
    }

    /**
     * "Try it" in the settings: rows of prints (yours, a horse's, a wolf's, a chicken's) stamped on the ground where you
     * look, made with the current settings.
     */
    public static void tryIt() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        net.minecraft.world.phys.HitResult hit = mc.player.pick(20, 1f, false);
        Vec3 at = hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK ? hit.getLocation() : mc.player.position();
        float yaw = mc.player.getYRot();
        double fx = -Math.sin(Math.toRadians(yaw)), fz = Math.cos(Math.toRadians(yaw));
        double rx = -Math.cos(Math.toRadians(yaw)), rz = -Math.sin(Math.toRadians(yaw));
        FootProfile[] lanes = {FootProfiles.of(mc.player), FootProfile.HORSE, FootProfile.WOLF, FootProfile.BIRD};
        long now = mc.level.getGameTime();
        for (int i = 0; i < lanes.length; i++) {
            if (lanes[i] == null) continue;
            double side = (i - 1.5) * 1.1;
            double sx = at.x + rx * side - fx * 2.5, sz = at.z + rz * side - fz * 2.5;
            TRACKER.demo(mc.level, mc.player, lanes[i], sx, at.y, sz, yaw, 5.0, now);
        }
    }

    /** {@code /footprints config}: the chat screen closes after the command, so open on the next tick. */
    public static void openConfigNextTick() {
        openConfig = true;
    }

    public static void tick(Minecraft mc) {
        long t0 = Bench.now();
        tickInner(mc);
        Bench.tick(t0);
    }

    private static void tickInner(Minecraft mc) {
        TestHooks.tick(mc);
        Recorder.tick(mc);
        if (openConfig) {
            openConfig = false;
            //? if >=26.2 {
            /*mc.gui.setScreen(configScreen(null));
            *///?} else
            mc.setScreen(configScreen(null));
        }
        ClientLevel level = mc.level;
        if (level != lastLevel) {
            POOL.clear();
            TRACKER.clear();
            WATER.clear();
            // block and entity tags belong to the world (datapacks): resolve surfaces and mobs again
            SurfaceRules.reset();
            FootProfiles.reset();
            lastLevel = level;
        }
        if (level != null && level.getGameTime() % 40 == 0 && PackData.reloaded()) {
            // F3+T or a pack switched: textures, shapes and rules again; pixel prints point at the old block atlas
            PackData.invalidate();
            FootprintAtlas.invalidate();
            SurfaceRules.reset();
            FootProfiles.reset();
            POOL.forEachAlive(p -> p.pixel = false);
        }
        if (level != null) FootprintAtlas.ensure();
        if (level == null || !FootprintsConfig.ENABLED.on() || mc.isPaused()) return;
        long now = level.getGameTime();
        TRACKER.tick(level, cameraPos(mc), now);
        WATER.tick(level, cameraPos(mc), now);
        // rain and snowfall wear exposed prints away faster
        int wear = level.isRaining() ? (int) Math.round(FootprintsConfig.RAIN_WEAR.value() - 1.0) : 0;
        POOL.tick(now, wear);
    }

    /** Unit vector the camera looks along. */
    public static org.joml.Vector3f lookVector(Minecraft mc) {
        //? if >=26.2 {
        /*return new org.joml.Vector3f(mc.gameRenderer.mainCamera().forwardVector());
        *///?} else if >=1.21.11 {
        /*return new org.joml.Vector3f(mc.gameRenderer.getMainCamera().forwardVector());
        *///?} else
        return new org.joml.Vector3f(mc.gameRenderer.getMainCamera().getLookVector());
    }

    public static Vec3 cameraPos(Minecraft mc) {
        //? if >=26.2 {
        /*return mc.gameRenderer.mainCamera().position();
        *///?} else if >=1.21.11 {
        /*return mc.gameRenderer.getMainCamera().position();
        *///?} else
        return mc.gameRenderer.getMainCamera().getPosition();
    }
}
