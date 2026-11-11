package online.farmsteadminecraft.footprints.brand;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;

/**
 * The Farmstead standard every mod of the line follows (one copy per mod, so no shared library jar):
 * a branded line in the log when the mod starts, and a quiet look on Modrinth for a newer release.
 *
 * <p>The check runs once, on a daemon thread, with short timeouts; it never blocks start-up and any
 * failure (offline, project not published yet, API change) is logged at debug level only. It sends
 * nothing but the request itself. Players and server owners turn it off with the mod's
 * {@code checkUpdates} setting, or for every Farmstead mod at once with {@code -Dfarmstead.updateCheck=false}
 * or the {@code FARMSTEAD_UPDATE_CHECK=false} environment variable.</p>
 */
public final class FarmsteadBrand {
    public static final String MODRINTH = "https://modrinth.com/mod/";
    private static final String API = "https://api.modrinth.com/v2/project/";

    private static volatile boolean started;
    private static volatile String latest;

    private FarmsteadBrand() {
    }

    /** {@code [Farmstead] Footprints 0.1.0 started (neoforge, Minecraft 1.21.1) - https://modrinth.com/mod/footprints} */
    public static void started(Logger log, String name, String version, String loader, String minecraft, String slug) {
        log.info("[Farmstead] {} {} started ({}, Minecraft {}) - {}{}", name, version, loader, minecraft, MODRINTH, slug);
    }

    /** False when the owner switched update checks off for all Farmstead mods. */
    public static boolean checksAllowed() {
        return !"false".equalsIgnoreCase(System.getProperty("farmstead.updateCheck"))
            && !"false".equalsIgnoreCase(System.getenv("FARMSTEAD_UPDATE_CHECK"));
    }

    /** The newest release seen on Modrinth for this loader and Minecraft version, or null (not checked / none). */
    public static String latest() {
        return latest;
    }

    /** Starts the check once per game session; repeated calls do nothing. */
    public static void checkForUpdate(Logger log, String name, String version, String loader, String minecraft, String slug) {
        if (started || !checksAllowed()) return;
        started = true;
        Thread thread = new Thread(() -> {
            try {
                check(log, name, version, loader, minecraft, slug);
            } catch (Exception e) {
                log.debug("[Farmstead] {} update check failed: {}", name, e.toString());
            }
        }, "Farmstead update check (" + slug + ")");
        thread.setDaemon(true);
        thread.start();
    }

    private static void check(Logger log, String name, String version, String loader, String minecraft, String slug)
        throws IOException {
        String query = "?loaders=" + enc("[\"" + loader + "\"]") + "&game_versions=" + enc("[\"" + minecraft + "\"]");
        HttpURLConnection http = (HttpURLConnection) URI.create(API + slug + "/version" + query).toURL().openConnection();
        http.setConnectTimeout(5000);
        http.setReadTimeout(8000);
        // Modrinth asks API clients to identify themselves
        http.setRequestProperty("User-Agent", "farmstead-mods/" + slug + "/" + version + " (farmsteadminecraft.online)");
        http.setRequestProperty("Accept", "application/json");
        if (http.getResponseCode() != 200) {
            log.debug("[Farmstead] {} update check: HTTP {}", name, http.getResponseCode());
            return;
        }
        JsonArray versions;
        try (InputStream in = http.getInputStream()) {
            versions = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonArray();
        }
        boolean wantPre = base(version).contains("-");
        String best = null;
        String bestNumber = null;
        for (JsonElement el : versions) {
            JsonObject v = el.getAsJsonObject();
            String type = v.has("version_type") ? v.get("version_type").getAsString() : "release";
            if (!"release".equals(type) && !wantPre) continue;
            String number = v.get("version_number").getAsString();
            if (best == null || compare(base(number), best) > 0) {
                best = base(number);
                bestNumber = number;
            }
        }
        if (best == null) {
            log.debug("[Farmstead] {}: no published versions for {} {}", name, loader, minecraft);
            return;
        }
        latest = best;
        if (compare(best, base(version)) > 0) {
            log.warn("[Farmstead] {} {} is available, this is {}. Download: {}{}/version/{}",
                name, best, base(version), MODRINTH, slug, bestNumber);
        } else {
            log.debug("[Farmstead] {} {} is the latest version", name, base(version));
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    /** {@code 0.2.0+1.21.1-neoforge} -> {@code 0.2.0}: the part before the build metadata. */
    static String base(String number) {
        int plus = number.indexOf('+');
        return (plus < 0 ? number : number.substring(0, plus)).trim();
    }

    /** Semantic order of {@code X.Y.Z[-pre]}: numbers compare as numbers, a pre-release sorts before its release. */
    static int compare(String a, String b) {
        String[] pa = a.split("-", 2), pb = b.split("-", 2);
        String[] na = pa[0].split("\\."), nb = pb[0].split("\\.");
        for (int i = 0; i < Math.max(na.length, nb.length); i++) {
            int x = i < na.length ? num(na[i]) : 0, y = i < nb.length ? num(nb[i]) : 0;
            if (x != y) return Integer.compare(x, y);
        }
        if (pa.length != pb.length) return pa.length < pb.length ? 1 : -1;
        return pa.length == 1 ? 0 : pa[1].compareTo(pb[1]);
    }

    private static int num(String s) {
        try {
            return Integer.parseInt(s.replaceAll("\\D.*", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
