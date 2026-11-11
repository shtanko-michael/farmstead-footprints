package online.farmsteadminecraft.footprints.config;

public final class IntValue extends ConfigValue<Integer> {
    public final int min;
    public final int max;

    IntValue(ConfigFile file, String section, String key, int def, int min, int max) {
        super(file, section, key, def);
        this.min = min;
        this.max = max;
    }

    public int value() {
        return value;
    }

    public void set(int v) {
        value = Math.max(min, Math.min(max, v));
    }

    @Override
    public String setRaw(String raw) {
        try {
            int v = Integer.parseInt(raw.trim().replace("_", ""));
            if (v < min || v > max) {
                reset();
                return problem(raw, "out of range " + min + " .. " + max);
            }
            value = v;
            return null;
        } catch (NumberFormatException e) {
            reset();
            return problem(raw, "not a whole number");
        }
    }

    @Override
    public String text() {
        return String.valueOf(value);
    }

    @Override
    public String defaultText() {
        return String.valueOf(defaultValue());
    }

    @Override
    public String rangeText() {
        return "range: " + min + " .. " + max;
    }
}
