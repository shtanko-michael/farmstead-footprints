package online.farmsteadminecraft.footprints.client;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import online.farmsteadminecraft.footprints.Footprints;

/**
 * Hooks for automated in-game tests, off unless the JVM runs with {@code -Dfarmstead.testhooks=true}
 * (the build adds it for {@code -PtestHooks}). The test runner drops {@code <name>.shot} files into
 * {@code <gameDir>/testhooks/}; each one becomes a screenshot named {@code <name>.png}. This avoids
 * sending keystrokes to a window that may not have focus.
 */
public final class TestHooks {
    private static final boolean ENABLED = Boolean.getBoolean("farmstead.testhooks");
    /** Forge 1.20.1 quickPlay joins as a vanilla client and never finishes the FML handshake,
     *  so there the runner passes the address here and the hook connects from the title screen. */
    private static final String JOIN = System.getProperty("farmstead.testhooks.join");
    private static boolean joined;

    private TestHooks() {
    }

    public static void tick(Minecraft mc) {
        // on with -Dfarmstead.testhooks=true, or when a <gameDir>/testhooks folder exists (capture rigs
        // whose launch arguments we do not control)
        if (!ENABLED && !Files.isDirectory(mc.gameDirectory.toPath().resolve("testhooks"))) return;
        //? if forge {
        /*if (JOIN != null && !joined && mc.level == null
            && mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen title) {
            joined = true;
            join(mc, title);
        }
        *///?}
        if (mc.level != null && mc.level.getGameTime() % 100 == 0) {
            Footprints.LOGGER.info("test hook stats: prints alive={} drawn last frame={} render calls={}",
                FootprintsClient.POOL.alive(), online.farmsteadminecraft.footprints.client.render.FootprintRenderer.lastDrawn,
                online.farmsteadminecraft.footprints.client.render.FootprintRenderer.frames);
            java.util.Map<String, Integer> byLane = new java.util.TreeMap<>();
            FootprintsClient.POOL.forEachAlive(p -> byLane.merge(p.shape + "@x" + (int) Math.floor(p.x / 10) * 10 + ",z" + (int) Math.floor(p.z), 1, Integer::sum));
            Footprints.LOGGER.info("test hook prints: {}", byLane);
        }
        Path dir = mc.gameDirectory.toPath().resolve("testhooks");
        if (!Files.isDirectory(dir)) return;
        try (Stream<Path> files = Files.list(dir)) {
            for (Path req : files.toList()) {
                String file = req.getFileName().toString();
                if (file.endsWith(".bench")) {
                    String secs = Files.readString(req).trim();
                    Files.deleteIfExists(req);
                    Bench.start(file.substring(0, file.length() - 6), secs.isEmpty() ? 10 : Integer.parseInt(secs));
                } else if (file.endsWith(".shot")) {
                    Files.deleteIfExists(req);
                    shoot(mc, file.substring(0, file.length() - 5));
                } else if (file.equals("config.open")) {
                    Files.deleteIfExists(req);
                    FootprintsClient.openConfigNextTick();
                } else if (file.equals("config.tab")) {
                    String tab = Files.readString(req).trim();
                    Files.deleteIfExists(req);
                    if (screen(mc) instanceof online.farmsteadminecraft.footprints.config.ConfigScreen cs) cs.showTab(tab);
                } else if (file.equals("config.pick")) {
                    // "<path> <search>"
                    String[] arg = Files.readString(req).trim().split(" ", 2);
                    Files.deleteIfExists(req);
                    if (screen(mc) instanceof online.farmsteadminecraft.footprints.config.ConfigScreen cs) cs.openPicker(arg[0], arg.length > 1 ? arg[1] : "");
                } else if (file.equals("try.it")) {
                    Files.deleteIfExists(req);
                    //? if >=26.2 {
                    /*mc.gui.setScreen(null);
                    *///?} else
                    mc.setScreen(null);
                    FootprintsClient.tryIt();
                } else if (file.equals("command.run")) {
                    // types a command as a player would (exercises the client command path of each loader)
                    String command = Files.readString(req).trim();
                    Files.deleteIfExists(req);
                    if (mc.player != null) mc.player.connection.sendCommand(command);
                } else if (file.equals("stress.fill")) {
                    Files.deleteIfExists(req);
                    stressFill(mc);
                } else if (file.equals("prints.clear")) {
                    Files.deleteIfExists(req);
                    FootprintsClient.POOL.clear();
                } else if (file.equals("prints.age")) {
                    // every print jumps to this share of its life (0..1): decay stages on demand
                    float share = Float.parseFloat(Files.readString(req).trim());
                    Files.deleteIfExists(req);
                    if (mc.level != null) {
                        long now = mc.level.getGameTime();
                        FootprintsClient.POOL.forEachAlive(p -> p.bornTick = now - (long) (share * p.lifeTicks));
                    }
                } else if (file.equals("model.feet")) {
                    // what the model reader makes of every mob in sight, next to the built-in profile
                    Files.deleteIfExists(req);
                    if (mc.level != null) {
                        java.util.Set<Object> seen = new java.util.HashSet<>();
                        for (net.minecraft.world.entity.Entity en : mc.level.entitiesForRendering()) {
                            if (en instanceof net.minecraft.world.entity.LivingEntity le && seen.add(le.getType())) {
                                FootProfile m = ModelFeet.of(le);
                                Footprints.LOGGER.info("model.feet {}: model={} builtIn={}", net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(le.getType()),
                                    m == null ? "none" : m.describe(), FootProfiles.of(le) == null ? "none" : FootProfiles.of(le).describe());
                            }
                        }
                    }
                } else if (file.equals("config.reload")) {
                    Files.deleteIfExists(req);
                    Footprints.LOGGER.info("test hook reload: problems={}", FootprintsConfig.load());
                }
            }
        } catch (Exception e) {
            Footprints.LOGGER.warn("test hook failed", e);
        }
    }

    //? if forge {
    /*private static void join(Minecraft mc, net.minecraft.client.gui.screens.Screen parent) {
        net.minecraft.client.gui.screens.ConnectScreen.startConnecting(parent, mc,
            net.minecraft.client.multiplayer.resolver.ServerAddress.parseString(JOIN),
            new net.minecraft.client.multiplayer.ServerData("test", JOIN, false), false);
    }
    *///?}

    /** Benchmark load: fills the whole pool with long-lived prints on the ground within 40 blocks of the player. */
    private static net.minecraft.client.gui.screens.Screen screen(Minecraft mc) {
        //? if >=26.2 {
        /*return mc.gui.screen();
        *///?} else
        return mc.screen;
    }

    private static void stressFill(Minecraft mc) {
        if (mc.level == null || mc.player == null) return;
        java.util.Random rnd = new java.util.Random(42);
        FootShape[] shapes = FootShape.builtIns().toArray(new FootShape[0]);
        long now = mc.level.getGameTime();
        int n = FootprintsClient.POOL.capacity();
        for (int i = 0; i < n; i++) {
            double x = mc.player.getX() + (rnd.nextDouble() - 0.5) * 80, z = mc.player.getZ() + (rnd.nextDouble() - 0.5) * 80;
            Footprint p = FootprintsClient.POOL.add();
            p.x = x;
            p.z = z;
            p.y = mc.level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
            p.yaw = rnd.nextFloat() * 360f;
            p.shape = shapes[rnd.nextInt(shapes.length - 1)];
            p.scale = 1f;
            p.depth = 0.8f;
            p.shadowRgb = 0x6B5A3A;
            p.rimRgb = 0xE0D2A8;
            p.bornTick = now;
            p.lifeTicks = 20 * 60 * 30;
            p.rim = 1f;
            p.exposed = false;
            p.prepare();
        }
        Footprints.LOGGER.info("test hook stress: {} prints", FootprintsClient.POOL.alive());
    }

    private static void shoot(Minecraft mc, String name) {
        File gameDir = mc.gameDirectory;
        //? if >=26.2 {
        /*Screenshot.grab(gameDir, name + ".png", mc.gameRenderer.mainRenderTarget(), 1, msg -> Footprints.LOGGER.info("test hook: {}", msg.getString()));
        *///?} else if >=1.21.11 {
        /*Screenshot.grab(gameDir, name + ".png", mc.getMainRenderTarget(), 1, msg -> Footprints.LOGGER.info("test hook: {}", msg.getString()));
        *///?} else
        Screenshot.grab(gameDir, name + ".png", mc.getMainRenderTarget(), msg -> Footprints.LOGGER.info("test hook: {}", msg.getString()));
    }
}
