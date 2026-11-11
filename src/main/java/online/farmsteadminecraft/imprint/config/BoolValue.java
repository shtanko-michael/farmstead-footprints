package online.farmsteadminecraft.imprint.config;

public final class BoolValue extends ConfigValue<Boolean> {
    BoolValue(ConfigFile file, String section, String key, boolean def) {
        super(file, section, key, def);
    }

    public boolean on() {
        return value;
    }

    public void set(boolean v) {
        value = v;
    }

    @Override
    public String setRaw(String raw) {
        String t = ConfigFile.lower(raw.trim());
        if (t.equals("true") || t.equals("false")) {
            value = t.equals("true");
            return null;
        }
        reset();
        return problem(raw, "expected true or false");
    }

    @Override
    public String text() {
        return String.valueOf(value);
    }

    @Override
    public String defaultText() {
        return String.valueOf(defaultValue());
    }
}
