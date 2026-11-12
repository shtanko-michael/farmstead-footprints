package online.farmsteadminecraft.footprints.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * One config file of a mod ({@code farmstead-<modid>-client.toml}, {@code -server}, {@code -common}).
 *
 * <p>Values are declared in code with a comment, a range and a default; the file is a flat TOML
 * subset (sections, {@code key = value}, {@code #} comments) that we read and write ourselves so
 * the same code runs on every loader and Minecraft version. Loading never throws on a bad value:
 * the value falls back to its default and the problem is reported, so a typo costs a message in
 * chat, not a crash on startup.
 *
 * <p>Presets set many values at once ({@code preset = "performance"}); a value the player wrote
 * by hand after picking a preset wins over the preset.
 */
public final class ConfigFile {
    public static final String PRESET_KEY = "preset";
    public static final String VERSION_KEY = "configVersion";
    private static final String NL = "\n";

    private final String modId;
    private final String side;
    private final int version;
    private final List<ConfigValue<?>> values = new ArrayList<>();
    private final Map<String, Map<String, Object>> presets = new LinkedHashMap<>();
    private final Map<String, String> renamed = new LinkedHashMap<>();
    private String preset = "default";
    /** The command named in the file header ({@code /<command> reload}); the mod id unless set. */
    private String command;

    public ConfigFile(String modId, String side, int version) {
        this.modId = modId;
        this.side = side;
        this.version = version;
        presets.put("default", Map.of());
    }

    /**
     * {@code farmstead-<modid>-<side>.toml}: every Farmstead mod's files start with "farmstead" so an
     * admin finds all of them together in a config folder of hundreds.
     */
    public String fileName() {
        String base = modId.startsWith("farmstead") ? modId : "farmstead-" + modId;
        return base + "-" + side + ".toml";
    }

    /** The name used before the "farmstead-" prefix; an old file is renamed on first load. */
    private String legacyFileName() {
        return modId + "-" + side + ".toml";
    }

    public String modId() {
        return modId;
    }

    public List<ConfigValue<?>> values() {
        return values;
    }

    public String preset() {
        return preset;
    }

    public java.util.Set<String> presetNames() {
        return presets.keySet();
    }

    public <V extends ConfigValue<?>> V add(V value) {
        values.add(value);
        return value;
    }

    public BoolValue bool(String section, String key, boolean def) {
        return add(new BoolValue(this, section, key, def));
    }

    public IntValue integer(String section, String key, int def, int min, int max) {
        return add(new IntValue(this, section, key, def, min, max));
    }

    public DoubleValue number(String section, String key, double def, double min, double max) {
        return add(new DoubleValue(this, section, key, def, min, max));
    }

    public <E extends Enum<E>> EnumValue<E> choice(String section, String key, E def) {
        return add(new EnumValue<>(this, section, key, def));
    }

    public StringListValue list(String section, String key, List<String> def) {
        return add(new StringListValue(this, section, key, def));
    }

    /** A preset: {@code "section.key" -> value} overrides on top of the defaults. */
    /** The mod's command, for the "reload in game" hint in the file header. */
    public ConfigFile command(String name) {
        this.command = name;
        return this;
    }

    public ConfigFile preset(String name, Map<String, Object> overrides) {
        presets.put(name, overrides);
        return this;
    }

    /** Migration: a key that used to be called {@code oldPath} ({@code section.key}). */
    public ConfigFile renamed(String oldPath, String newPath) {
        renamed.put(oldPath, newPath);
        return this;
    }

    public ConfigValue<?> find(String path) {
        for (ConfigValue<?> v : values) {
            if (v.path().equals(path)) return v;
        }
        return null;
    }

    /** Resets every value to the selected preset (defaults plus the preset's overrides). */
    public List<String> applyPreset(String name) {
        List<String> problems = new ArrayList<>();
        Map<String, Object> overrides = presets.get(name);
        if (overrides == null) {
            problems.add("unknown preset \"" + name + "\" (known: " + String.join(", ", presets.keySet()) + ")");
            name = "default";
            overrides = Map.of();
        }
        preset = name;
        for (ConfigValue<?> v : values) v.reset();
        for (Map.Entry<String, Object> e : overrides.entrySet()) {
            ConfigValue<?> v = find(e.getKey());
            if (v == null) throw new IllegalStateException("preset " + name + " names unknown key " + e.getKey());
            String problem = v.setRaw(String.valueOf(e.getValue()));
            if (problem != null) throw new IllegalStateException("preset " + name + ": " + problem);
        }
        return problems;
    }

    /**
     * Reads the file (creating it with defaults if missing), then writes it back so new keys and
     * comments appear. Returns human-readable problems; never throws for content errors.
     */
    public List<String> load(Path dir) {
        Path file = dir.resolve(fileName());
        List<String> problems = new ArrayList<>();
        Path legacy = dir.resolve(legacyFileName());
        if (!Files.exists(file) && Files.exists(legacy)) {
            try {
                Files.move(legacy, file);
            } catch (IOException e) {
                problems.add("cannot rename " + legacyFileName() + " to " + fileName() + ": " + e.getMessage());
            }
        }
        Map<String, String> raw = new LinkedHashMap<>();
        if (Files.exists(file)) {
            try {
                raw = Toml.parse(Files.readAllLines(file, StandardCharsets.UTF_8), problems);
            } catch (IOException e) {
                problems.add("cannot read " + fileName() + ": " + e.getMessage());
            }
        }
        String presetRaw = raw.remove(PRESET_KEY);
        raw.remove(VERSION_KEY);
        problems.addAll(applyPreset(presetRaw == null ? "default" : Toml.unquote(presetRaw)));
        for (Map.Entry<String, String> e : raw.entrySet()) {
            String path = renamed.getOrDefault(e.getKey(), e.getKey());
            ConfigValue<?> v = find(path);
            if (v == null) {
                problems.add(fileName() + ": unknown key " + e.getKey() + " (ignored)");
                continue;
            }
            String before = v.text();
            String problem = v.setRaw(e.getValue());
            if (problem != null) {
                v.setRaw(before); // a bad line keeps what the preset gave, not the bare default
                problems.add(fileName() + ": " + problem + ", keeping " + before);
            }
        }
        save(dir, problems);
        return problems;
    }

    public void save(Path dir, List<String> problems) {
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(fileName()), render(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            problems.add("cannot write " + fileName() + ": " + e.getMessage());
        }
    }

    /**
     * The file text: header, preset, then each section with commented keys. A value that equals
     * what the preset gives is written commented out, so switching the preset later still moves
     * it; only values the player actually changed are active lines and stay pinned.
     */
    public String render() {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(modId).append(" ").append(side).append(" settings").append(NL);
        sb.append("# Pick a preset; lines starting with '#key' follow the preset.").append(NL);
        sb.append("# Remove the '#' in front of a key to set it yourself; your value then wins over the preset.").append(NL);
        sb.append("# Presets: ").append(String.join(", ", presets.keySet())).append(NL);
        sb.append("# Reload in game with /").append(command != null ? command : modId).append(" reload").append(NL).append(NL);
        sb.append(PRESET_KEY).append(" = \"").append(preset).append('"').append(NL);
        sb.append(VERSION_KEY).append(" = ").append(version).append(NL);
        Map<String, Object> overrides = presets.getOrDefault(preset, Map.of());
        Map<String, List<ConfigValue<?>>> bySection = new LinkedHashMap<>();
        for (ConfigValue<?> v : values) bySection.computeIfAbsent(v.section(), k -> new ArrayList<>()).add(v);
        for (Map.Entry<String, List<ConfigValue<?>>> section : bySection.entrySet()) {
            sb.append(NL).append('[').append(section.getKey()).append(']').append(NL);
            for (ConfigValue<?> v : section.getValue()) {
                sb.append(NL);
                for (String line : v.comment().split(NL)) sb.append("# ").append(line).append(NL);
                String range = v.rangeText();
                sb.append("# ").append(range.isEmpty() ? "" : range + ", ").append("default: ").append(v.defaultText()).append(NL);
                boolean followsPreset = v.text().equals(presetText(v, overrides));
                sb.append(followsPreset ? "#" : "").append(v.key()).append(" = ").append(v.text()).append(NL);
            }
        }
        return sb.toString();
    }

    /** What the active preset gives this value, in file form. */
    private static String presetText(ConfigValue<?> v, Map<String, Object> overrides) {
        Object o = overrides.get(v.path());
        if (o == null) return v.defaultText();
        String current = v.text();
        v.setRaw(String.valueOf(o));
        String text = v.text();
        v.setRaw(current);
        return text;
    }

    static String lower(String s) {
        return s.toLowerCase(Locale.ROOT);
    }
}
