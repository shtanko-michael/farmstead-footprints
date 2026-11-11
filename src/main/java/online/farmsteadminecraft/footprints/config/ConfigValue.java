package online.farmsteadminecraft.footprints.config;

/**
 * One setting: where it lives ({@code section.key}), its default, its current value and how it
 * reads from and writes to the file. The screen and the file both go through {@link #setRaw}, so a
 * value typed in the file and one picked on the screen are validated the same way.
 *
 * <p>Display name and tooltip are translation keys {@code config.<modid>.<section>.<key>} and
 * {@code ...tooltip}; the English comment written into the file is given in code.
 */
public abstract class ConfigValue<T> {
    private final ConfigFile file;
    private final String section;
    private final String key;
    private final T def;
    private String comment = "";
    private boolean syncedToClient;
    protected T value;

    protected ConfigValue(ConfigFile file, String section, String key, T def) {
        this.file = file;
        this.section = section;
        this.key = key;
        this.def = def;
        this.value = def;
    }

    @SuppressWarnings("unchecked")
    public <S extends ConfigValue<T>> S comment(String text) {
        this.comment = text;
        return (S) this;
    }

    /** Server value the client must follow (sent on join and after reload). */
    @SuppressWarnings("unchecked")
    public <S extends ConfigValue<T>> S synced() {
        this.syncedToClient = true;
        return (S) this;
    }

    public T get() {
        return value;
    }

    public T defaultValue() {
        return def;
    }

    public void reset() {
        value = def;
    }

    public String section() {
        return section;
    }

    public String key() {
        return key;
    }

    public String path() {
        return section + "." + key;
    }

    public String comment() {
        return comment;
    }

    public boolean isSynced() {
        return syncedToClient;
    }

    public String translationKey() {
        return "config." + file.modId() + "." + path();
    }

    /** Parses and validates a file token; returns a problem message or {@code null} on success. */
    public abstract String setRaw(String raw);

    /** The current value as a file token. */
    public abstract String text();

    public abstract String defaultText();

    /** e.g. {@code "range: 8 .. 128"}; empty when there is no range. */
    public String rangeText() {
        return "";
    }

    protected String problem(String raw, String why) {
        return path() + " = " + raw + ": " + why;
    }
}
