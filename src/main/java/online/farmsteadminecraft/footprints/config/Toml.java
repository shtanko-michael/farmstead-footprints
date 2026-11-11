package online.farmsteadminecraft.footprints.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The TOML subset our config files use: {@code [section]} headers, {@code key = value} lines,
 * {@code #} comments, values that are booleans, numbers, "strings" or ["string", "lists"] on one
 * line. Returns raw value tokens keyed {@code section.key} (top-level keys have no section prefix).
 */
final class Toml {
    private Toml() {
    }

    static Map<String, String> parse(List<String> lines, List<String> problems) {
        Map<String, String> out = new LinkedHashMap<>();
        String section = "";
        int n = 0;
        for (String line : lines) {
            n++;
            String t = stripComment(line).trim();
            if (t.isEmpty()) continue;
            if (t.startsWith("[") && t.endsWith("]") && !t.contains("=")) {
                section = t.substring(1, t.length() - 1).trim();
                continue;
            }
            int eq = t.indexOf('=');
            if (eq <= 0) {
                problems.add("line " + n + ": expected key = value, got \"" + line.trim() + "\"");
                continue;
            }
            String key = t.substring(0, eq).trim();
            String value = t.substring(eq + 1).trim();
            out.put(section.isEmpty() ? key : section + "." + key, value);
        }
        return out;
    }

    /** Drops a trailing {@code # comment} that is not inside a string. */
    private static String stripComment(String line) {
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"' && (i == 0 || line.charAt(i - 1) != '\\')) quoted = !quoted;
            if (c == '#' && !quoted) return line.substring(0, i);
        }
        return line;
    }

    static String unquote(String raw) {
        String t = raw.trim();
        if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
            return t.substring(1, t.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
        }
        return t;
    }

    /** {@code ["a", "b"]} to a list; {@code null} if it is not a list. */
    static List<String> parseList(String raw) {
        String t = raw.trim();
        if (!t.startsWith("[") || !t.endsWith("]")) return null;
        String body = t.substring(1, t.length() - 1).trim();
        List<String> out = new ArrayList<>();
        if (body.isEmpty()) return out;
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '"' && (i == 0 || body.charAt(i - 1) != '\\')) {
                quoted = !quoted;
                cur.append(c);
            } else if (c == ',' && !quoted) {
                out.add(unquote(cur.toString()));
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        if (!cur.toString().isBlank()) out.add(unquote(cur.toString()));
        return out;
    }
}
