package online.farmsteadminecraft.footprints.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import online.farmsteadminecraft.footprints.Footprints;

/**
 * Test rig only (TestHooks {@code record}): every client tick the last frame is saved as a numbered PNG in
 * {@code <gameDir>/recordings/<name>/}, for the listing's GIFs. Files are written on worker threads so the game keeps
 * its pace. Available where the frame can be read back synchronously (before 1.21.5).
 */
final class Recorder {
    private static final ExecutorService WRITERS = Executors.newFixedThreadPool(3, r -> {
        Thread t = new Thread(r, "footprints-recorder");
        t.setDaemon(true);
        return t;
    });
    private static Path dir;
    private static int left;
    private static int frame;

    private Recorder() {
    }

    static void start(Minecraft mc, String name, float seconds) {
        try {
            dir = mc.gameDirectory.toPath().resolve("recordings").resolve(name);
            Files.createDirectories(dir);
            try (var files = Files.list(dir)) {
                files.forEach(f -> f.toFile().delete());
            }
            left = Math.round(seconds * 20);
            frame = 0;
            Footprints.LOGGER.info("recording {} for {} ticks", name, left);
        } catch (Exception e) {
            Footprints.LOGGER.warn("recording could not start", e);
            left = 0;
        }
    }

    static boolean active() {
        return left > 0;
    }

    static void tick(Minecraft mc) {
        if (left <= 0) return;
        left--;
        //? if <1.21.5 {
        NativeImage image = net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget());
        Path file = dir.resolve(String.format("%05d.png", frame++));
        WRITERS.submit(() -> {
            try (NativeImage img = image) {
                img.writeToFile(file);
            } catch (Exception e) {
                Footprints.LOGGER.warn("recording frame failed: {}", e.toString());
            }
        });
        //?} else {
        /*if (frame++ == 0) Footprints.LOGGER.warn("recording is only wired up before 1.21.5");
        *///?}
        if (left == 0) Footprints.LOGGER.info("recording done: {} frames", frame);
    }
}
