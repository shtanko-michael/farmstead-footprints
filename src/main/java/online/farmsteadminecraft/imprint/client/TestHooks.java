package online.farmsteadminecraft.imprint.client;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import online.farmsteadminecraft.imprint.Imprint;

/**
 * Hooks for automated in-game tests, off unless the JVM runs with {@code -Dfarmstead.testhooks=true}
 * (the build adds it for {@code -PtestHooks}). The test runner drops {@code <name>.shot} files into
 * {@code <gameDir>/testhooks/}; each one becomes a screenshot named {@code <name>.png}. This avoids
 * sending keystrokes to a window that may not have focus.
 */
public final class TestHooks {
    private static final boolean ENABLED = Boolean.getBoolean("farmstead.testhooks");

    private TestHooks() {
    }

    public static void tick(Minecraft mc) {
        if (!ENABLED) return;
        if (mc.level != null && mc.level.getGameTime() % 100 == 0) {
            Imprint.LOGGER.info("test hook stats: prints alive={} drawn last frame={} render calls={}",
                ImprintClient.POOL.alive(), online.farmsteadminecraft.imprint.client.render.FootprintRenderer.lastDrawn,
                online.farmsteadminecraft.imprint.client.render.FootprintRenderer.frames);
            java.util.Map<String, Integer> byLane = new java.util.TreeMap<>();
            ImprintClient.POOL.forEachAlive(p -> byLane.merge(p.shape + "@x" + (int) Math.floor(p.x / 10) * 10 + ",z" + (int) Math.floor(p.z), 1, Integer::sum));
            Imprint.LOGGER.info("test hook prints: {}", byLane);
        }
        Path dir = mc.gameDirectory.toPath().resolve("testhooks");
        if (!Files.isDirectory(dir)) return;
        try (Stream<Path> files = Files.list(dir)) {
            for (Path req : files.filter(p -> p.toString().endsWith(".shot")).toList()) {
                String name = req.getFileName().toString().replace(".shot", "");
                Files.deleteIfExists(req);
                shoot(mc, name);
            }
        } catch (Exception e) {
            Imprint.LOGGER.warn("test hook failed", e);
        }
    }

    private static void shoot(Minecraft mc, String name) {
        File gameDir = mc.gameDirectory;
        //? if >=26.1 {
        /*Screenshot.grab(gameDir, name + ".png", mc.gameRenderer.mainRenderTarget(), 1, msg -> Imprint.LOGGER.info("test hook: {}", msg.getString()));
        *///?} else
        Screenshot.grab(gameDir, name + ".png", mc.getMainRenderTarget(), msg -> Imprint.LOGGER.info("test hook: {}", msg.getString()));
    }
}
