package online.farmsteadminecraft.footprints.client;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import online.farmsteadminecraft.footprints.Footprints;

/**
 * What resource packs (and mods, whose assets are a pack too) add: print shapes as pictures in
 * {@code assets/<ns>/textures/farmstead_footprints/shapes/<name>.png}, the two base textures replaced at
 * {@code assets/farmstead_footprints/textures/footprints_shadow.png} / {@code footprints_rim.png}, and rules in
 * {@code assets/<ns>/farmstead_footprints/profiles/<any>.json}:
 *
 * <pre>{
 *   "profiles": ["mymod:yeti = shape:big size:1.4", "#mymod:birds = shape:bird"],
 *   "surfaces": {"include": ["mymod:ash_block"], "exclude": ["mymod:crystal_floor"]}
 * }</pre>
 *
 * The player's own config comes first: its lines win over a pack's. F3+T (or any pack change) is noticed and
 * everything is read again.
 */
public final class PackData {
    public static final String SHAPES = "textures/farmstead_footprints/shapes";
    private static final String RULES = "farmstead_footprints/profiles";

    private static boolean loaded;
    private static final List<String> profiles = new ArrayList<>();
    private static final List<String> include = new ArrayList<>();
    private static final List<String> exclude = new ArrayList<>();
    private static int signature;

    private PackData() {
    }

    public static List<String> profiles() {
        ensure();
        return profiles;
    }

    public static List<String> include() {
        ensure();
        return include;
    }

    public static List<String> exclude() {
        ensure();
        return exclude;
    }

    public static void invalidate() {
        loaded = false;
    }

    /** True once after the client's resources were reloaded (F3+T, a pack switched on or off). */
    public static boolean reloaded() {
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        int sig = rm.listPacks().mapToInt(System::identityHashCode).reduce(17, (a, b) -> a * 31 + b);
        if (signature == 0) {
            signature = sig;
            return false;
        }
        if (sig == signature) return false;
        signature = sig;
        return true;
    }

    private static void ensure() {
        if (loaded) return;
        loaded = true;
        profiles.clear();
        include.clear();
        exclude.clear();
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        Map<ResourceLocation, Resource> files = rm.listResources(RULES, id -> id.getPath().endsWith(".json"));
        for (Map.Entry<ResourceLocation, Resource> f : files.entrySet()) {
            try (InputStream in = f.getValue().open(); Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
                strings(root.get("profiles"), profiles);
                if (root.has("surfaces")) {
                    JsonObject s = root.getAsJsonObject("surfaces");
                    strings(s.get("include"), include);
                    strings(s.get("exclude"), exclude);
                }
            } catch (Exception e) {
                Footprints.LOGGER.warn("resource pack rules {} could not be read: {}", f.getKey(), e.toString());
            }
        }
        if (!files.isEmpty()) {
            Footprints.LOGGER.info("resource packs: {} rule file(s), {} profile(s), {} surface rule(s)", files.size(),
                profiles.size(), include.size() + exclude.size());
        }
    }

    private static void strings(JsonElement e, List<String> out) {
        if (e == null || !e.isJsonArray()) return;
        JsonArray a = e.getAsJsonArray();
        for (JsonElement x : a) out.add(x.getAsString());
    }

    /** A base texture from the packs (a pack may replace it), or null to use the copy in the jar. */
    static InputStream packTexture(String file) {
        try {
            return Minecraft.getInstance().getResourceManager().getResource(Footprints.id("textures/" + file))
                .map(res -> {
                    try {
                        return res.open();
                    } catch (Exception ex) {
                        return null;
                    }
                }).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    /** Shape pictures from the packs, by name (file name without .png). */
    static Map<ResourceLocation, Resource> packShapes() {
        return Minecraft.getInstance().getResourceManager().listResources(SHAPES, id -> id.getPath().endsWith(".png"));
    }
}
