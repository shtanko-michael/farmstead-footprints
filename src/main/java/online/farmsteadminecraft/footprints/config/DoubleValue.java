package online.farmsteadminecraft.footprints.config;

import java.util.Locale;

public final class DoubleValue extends ConfigValue<Double> {
    public final double min;
    public final double max;

    DoubleValue(ConfigFile file, String section, String key, double def, double min, double max) {
        super(file, section, key, def);
        this.min = min;
        this.max = max;
    }

    public double value() {
        return value;
    }

    public float floatValue() {
        return (float) (double) value;
    }

    public void set(double v) {
        value = Math.max(min, Math.min(max, v));
    }

    @Override
    public String setRaw(String raw) {
        try {
            double v = Double.parseDouble(raw.trim().replace("_", ""));
            if (Double.isNaN(v) || v < min || v > max) {
                reset();
                return problem(raw, "out of range " + fmt(min) + " .. " + fmt(max));
            }
            value = v;
            return null;
        } catch (NumberFormatException e) {
            reset();
            return problem(raw, "not a number");
        }
    }

    static String fmt(double v) {
        String s = String.format(Locale.ROOT, "%.3f", v);
        s = s.replaceAll("0+$", "");
        return s.endsWith(".") ? s + "0" : s;
    }

    @Override
    public String text() {
        return fmt(value);
    }

    @Override
    public String defaultText() {
        return fmt(defaultValue());
    }

    @Override
    public String rangeText() {
        return "range: " + fmt(min) + " .. " + fmt(max);
    }
}
