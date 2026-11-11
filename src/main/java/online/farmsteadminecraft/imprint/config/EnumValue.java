package online.farmsteadminecraft.imprint.config;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class EnumValue<E extends Enum<E>> extends ConfigValue<E> {
    private final E[] options;

    EnumValue(ConfigFile file, String section, String key, E def) {
        super(file, section, key, def);
        this.options = def.getDeclaringClass().getEnumConstants();
    }

    public E[] options() {
        return options;
    }

    public void set(E v) {
        value = v;
    }

    public E next() {
        return options[(value.ordinal() + 1) % options.length];
    }

    @Override
    public String setRaw(String raw) {
        String t = ConfigFile.lower(Toml.unquote(raw.trim()));
        for (E e : options) {
            if (ConfigFile.lower(e.name()).equals(t)) {
                value = e;
                return null;
            }
        }
        reset();
        return problem(raw, "expected one of " + names());
    }

    private String names() {
        return Arrays.stream(options).map(e -> ConfigFile.lower(e.name())).collect(Collectors.joining(", "));
    }

    @Override
    public String text() {
        return "\"" + ConfigFile.lower(value.name()) + "\"";
    }

    @Override
    public String defaultText() {
        return "\"" + ConfigFile.lower(defaultValue().name()) + "\"";
    }

    @Override
    public String rangeText() {
        return "one of: " + names();
    }
}
