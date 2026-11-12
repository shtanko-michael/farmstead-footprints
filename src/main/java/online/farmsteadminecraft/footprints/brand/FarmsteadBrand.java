package online.farmsteadminecraft.footprints.brand;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;

/**
 * The Farmstead standard every mod of the line follows (one copy per mod, so no shared library jar):
 * a branded line in the log when the mod starts, and a quiet look for a newer release.
 *
 * <p>The check runs once, on a daemon thread, with short timeouts; it never blocks start-up and never throws.
 * It asks Modrinth first and CurseForge when Modrinth cannot answer (unreachable, project not public, no
 * versions for this loader and Minecraft), and writes exactly one line to the log: the latest version, a newer
 * one with its download link, or that neither catalogue could be reached. It sends nothing but the request
 * itself. Players and server owners turn it off with the mod's {@code checkUpdates} setting, or for every
 * Farmstead mod at once with {@code -Dfarmstead.updateCheck=false} or the {@code FARMSTEAD_UPDATE_CHECK=false}
 * environment variable.</p>
 */
public final class FarmsteadBrand {
    public static final String MODRINTH = "https://modrinth.com/mod/";
    public static final String CURSEFORGE = "https://www.curseforge.com/minecraft/mc-mods/";
    private static final String MODRINTH_API = "https://api.modrinth.com/v2/project/";
    /** The public site endpoint: no API key needed. */
    private static final String CURSEFORGE_API = "https://www.curseforge.com/api/v1/mods/";
    /** {@code <file>-<loader>-<version>-<minecraft>.jar}, the name every Farmstead jar carries on CurseForge. */
    private static final Pattern CF_FILE = Pattern.compile("-(fabric|quilt|neoforge|forge)-(.+)-[^-]+\\.jar$");
    /** Overridable for tests only: an unreachable host checks the offline path. */
    private static final String MODRINTH_BASE = System.getProperty("farmstead.updateCheck.modrinth", MODRINTH_API);
    private static final String CURSEFORGE_BASE = System.getProperty("farmstead.updateCheck.curseforge", CURSEFORGE_API);

    private static volatile boolean started;
    private static volatile String latest;
    private static volatile String source;

    private FarmsteadBrand() {
    }

    /** What a mod is called in the catalogues: the Modrinth and CurseForge slug, and the CurseForge project id ("" if none yet). */
    public record Project(String name, String version, String loader, String minecraft, String slug, String curseforgeId) {
    }

    /** {@code [Farmstead] Footprints 0.1.0 started (neoforge, Minecraft 1.21.1) - https://modrinth.com/mod/farmstead-footprints} */
    public static void started(Logger log, String name, String version, String loader, String minecraft, String slug) {
        log.info("[Farmstead] {} {} started ({}, Minecraft {}) - {}{}", name, version, loader, minecraft, MODRINTH, slug);
    }

    /** False when the owner switched update checks off for all Farmstead mods. */
    public static boolean checksAllowed() {
        return !"false".equalsIgnoreCase(System.getProperty("farmstead.updateCheck"))
            && !"false".equalsIgnoreCase(System.getenv("FARMSTEAD_UPDATE_CHECK"));
    }

    /** The newest release seen in a catalogue for this loader and Minecraft version, or null (not checked / none). */
    public static String latest() {
        return latest;
    }

    /** The catalogue that answered ("Modrinth" or "CurseForge"), or null if none did. */
    public static String source() {
        return source;
    }

    /** For a settings screen: null if not checked, "" if {@code version} is the newest, else the newer version. */
    public static String newerThan(String version) {
        String l = latest;
        if (l == null) return null;
        return compare(l, base(version)) > 0 ? l : "";
    }

    /** One line saying the check is off (the mod's own setting); repeated calls do nothing. */
    public static void off(Logger log, String name) {
        if (started) return;
        started = true;
        log.info("[Farmstead] {} update check off", name);
    }

    /** Starts the check once per game session; repeated calls do nothing. */
    public static void checkForUpdate(Logger log, Project p) {
        if (started) return;
        if (!checksAllowed()) {
            off(log, p.name());
            return;
        }
        started = true;
        Thread thread = new Thread(() -> {
            try {
                check(log, p);
            } catch (Throwable e) {
                log.info("[Farmstead] {} update check: could not reach Modrinth or CurseForge ({})", p.name(), reason(e));
            }
        }, "Farmstead update check (" + p.slug() + ")");
        thread.setDaemon(true);
        thread.start();
    }

    private static void check(Logger log, Project p) {
        String mrFail;
        String[] found = null;
        try {
            found = modrinth(p);
            mrFail = found == null ? "Modrinth: no versions for " + p.loader() + " " + p.minecraft() : null;
        } catch (Exception e) {
            mrFail = "Modrinth: " + reason(e);
        }
        String cfFail = null;
        if (found == null) {
            if (p.curseforgeId() == null || p.curseforgeId().isBlank()) {
                cfFail = "CurseForge: no project id";
            } else {
                try {
                    found = curseforge(p);
                    if (found == null) cfFail = "CurseForge: no versions for " + p.loader() + " " + p.minecraft();
                } catch (Exception e) {
                    cfFail = "CurseForge: " + reason(e);
                }
            }
        }
        if (found == null) {
            log.info("[Farmstead] {} update check: could not reach Modrinth or CurseForge ({}; {})", p.name(), mrFail, cfFail);
            return;
        }
        String best = found[0], from = found[2];
        latest = best;
        source = from;
        if (compare(best, base(p.version())) > 0) {
            log.warn("[Farmstead] {} {} is available, this is {}. Download: {}", p.name(), best, base(p.version()), found[1]);
        } else {
            log.info("[Farmstead] {} {} is the latest version (checked on {})", p.name(), base(p.version()), from);
        }
    }

    /** {version, download url, "Modrinth"}, or null when Modrinth has no release for this loader and Minecraft. */
    private static String[] modrinth(Project p) throws IOException {
        String query = "?loaders=" + enc("[\"" + p.loader() + "\"]") + "&game_versions=" + enc("[\"" + p.minecraft() + "\"]");
        JsonArray versions = get(MODRINTH_BASE + p.slug() + "/version" + query, p).getAsJsonArray();
        boolean wantPre = base(p.version()).contains("-");
        String best = null, bestNumber = null;
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
        return best == null ? null : new String[] {best, MODRINTH + p.slug() + "/version/" + bestNumber, "Modrinth"};
    }

    /** The same from the CurseForge file list: the loader and Minecraft version are tags, the version is in the file name. */
    private static String[] curseforge(Project p) throws IOException {
        JsonArray files = get(CURSEFORGE_BASE + p.curseforgeId() + "/files?pageIndex=0&pageSize=50", p)
            .getAsJsonObject().getAsJsonArray("data");
        boolean wantPre = base(p.version()).contains("-");
        String best = null, bestId = null;
        for (JsonElement el : files) {
            JsonObject f = el.getAsJsonObject();
            // 1 release, 2 beta, 3 alpha
            if (f.has("releaseType") && f.get("releaseType").getAsInt() != 1 && !wantPre) continue;
            boolean loader = false, mc = false;
            for (JsonElement tag : f.getAsJsonArray("gameVersions")) {
                String t = tag.getAsString();
                loader |= t.equalsIgnoreCase(p.loader());
                mc |= t.equals(p.minecraft());
            }
            if (!loader || !mc) continue;
            Matcher m = CF_FILE.matcher(f.get("fileName").getAsString());
            if (!m.find() || !m.group(1).equalsIgnoreCase(p.loader())) continue;
            String number = m.group(2);
            if (best == null || compare(number, best) > 0) {
                best = number;
                bestId = f.get("id").getAsString();
            }
        }
        return best == null ? null : new String[] {best, CURSEFORGE + p.slug() + "/files/" + bestId, "CurseForge"};
    }

    private static JsonElement get(String url, Project p) throws IOException {
        HttpURLConnection http = (HttpURLConnection) URI.create(url).toURL().openConnection();
        http.setConnectTimeout(5000);
        http.setReadTimeout(8000);
        // the catalogues ask API clients to identify themselves
        http.setRequestProperty("User-Agent", "farmstead-mods/" + p.slug() + "/" + p.version() + " (farmsteadminecraft.online)");
        http.setRequestProperty("Accept", "application/json");
        try {
            int code = http.getResponseCode();
            if (code != 200) throw new IOException("HTTP " + code);
            try (InputStream in = http.getInputStream()) {
                return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } finally {
            http.disconnect();
        }
    }

    /** A short reason for the log line: the exception's class and message, no stack trace. */
    private static String reason(Throwable e) {
        String msg = e.getMessage();
        if (e instanceof IOException && msg != null && msg.startsWith("HTTP ")) return msg;
        String type = e.getClass().getSimpleName();
        return msg == null || msg.isBlank() ? type : type + ": " + (msg.length() > 120 ? msg.substring(0, 120) : msg);
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
