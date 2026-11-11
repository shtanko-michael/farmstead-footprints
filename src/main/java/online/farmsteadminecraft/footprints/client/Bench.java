package online.farmsteadminecraft.footprints.client;

import net.minecraft.client.Minecraft;
import online.farmsteadminecraft.footprints.Footprints;

/**
 * Frame-time measurement for the benchmark scene (test hooks only: {@code <label>.bench} in the
 * testhooks folder, the file holding the number of seconds). Counts frames and the nanoseconds the
 * mod itself spends drawing and ticking, then logs one line:
 * {@code bench <label>: fps=.. frameMs=.. renderUs=.. tickUs=.. alive=.. drawn=..}.
 * Costs nothing when no measurement runs.
 */
public final class Bench {
    private static boolean on;
    private static String label;
    private static long startNs, endNs, frames, renderNs, ticks, tickNs, drawn, fpsSum, fpsSamples;

    private Bench() {
    }

    static void start(String name, int seconds) {
        label = name;
        frames = renderNs = ticks = tickNs = drawn = fpsSum = fpsSamples = 0;
        startNs = System.nanoTime();
        endNs = startNs + seconds * 1_000_000_000L;
        on = true;
        Footprints.LOGGER.info("bench {}: started for {} s", name, seconds);
    }

    public static long now() {
        return on ? System.nanoTime() : 0L;
    }

    /** Once per rendered frame, before any early return, so frames count with the mod switched off too. */
    public static void frame() {
        if (on) frames++;
    }

    public static void render(long t0, int quads) {
        if (on && t0 != 0L) {
            renderNs += System.nanoTime() - t0;
            drawn += quads;
        }
    }

    public static void tick(long t0) {
        if (!on) return;
        if (t0 != 0L) {
            tickNs += System.nanoTime() - t0;
            ticks++;
        }
        // the game's own fps counter, sampled once a second: comparable with or without our mod running
        if (ticks % 20 == 0) {
            fpsSum += Minecraft.getInstance().getFps();
            fpsSamples++;
        }
        long t = System.nanoTime();
        if (t < endNs) return;
        on = false;
        double secs = (t - startNs) / 1e9;
        Footprints.LOGGER.info("bench {}: gameFps={} fps={} frameMs={} renderUs={} tickUs={} alive={} drawn={} (last vanilla fps {})",
            label, fmt(fpsSum / (double) Math.max(1, fpsSamples)), fmt(frames / secs), fmt(secs * 1000 / Math.max(1, frames)), fmt(renderNs / 1000.0 / Math.max(1, frames)),
            fmt(tickNs / 1000.0 / Math.max(1, ticks)), FootprintsClient.POOL.alive(), drawn / Math.max(1, frames),
            Minecraft.getInstance().getFps());
    }

    private static String fmt(double v) {
        return String.format(java.util.Locale.ROOT, "%.2f", v);
    }
}
