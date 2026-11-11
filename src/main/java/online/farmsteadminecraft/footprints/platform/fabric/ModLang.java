package online.farmsteadminecraft.footprints.platform.fabric;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import online.farmsteadminecraft.footprints.Footprints;

/** The mod's lang files read from the jar (see ClientLanguageMixin), each parsed once. */
public final class ModLang {
    private static final Map<String, Map<String, String>> CACHE = new HashMap<>();

    private ModLang() {
    }

    public static void addTo(String language, Map<String, String> out) {
        Map<String, String> ours = CACHE.computeIfAbsent(language, ModLang::read);
        for (Map.Entry<String, String> e : ours.entrySet()) out.putIfAbsent(e.getKey(), e.getValue());
    }

    private static Map<String, String> read(String language) {
        Map<String, String> map = new HashMap<>();
        String path = "/assets/" + Footprints.MOD_ID + "/lang/" + language + ".json";
        try (InputStream in = ModLang.class.getResourceAsStream(path)) {
            if (in == null) return map;
            JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : json.entrySet()) map.put(e.getKey(), e.getValue().getAsString());
        } catch (Exception e) {
            Footprints.LOGGER.warn("lang {}: {}", language, e.toString());
        }
        return map;
    }
}
