package online.farmsteadminecraft.footprints.config;

import java.util.List;
import java.util.stream.Collectors;

public final class StringListValue extends ConfigValue<List<String>> {
    StringListValue(ConfigFile file, String section, String key, List<String> def) {
        super(file, section, key, List.copyOf(def));
    }

    public boolean contains(String s) {
        return value.contains(s);
    }

    public void set(List<String> v) {
        value = List.copyOf(v);
    }

    @Override
    public String setRaw(String raw) {
        List<String> parsed = Toml.parseList(raw);
        if (parsed == null) {
            reset();
            return problem(raw, "expected a list like [\"a\", \"b\"]");
        }
        value = List.copyOf(parsed);
        return null;
    }

    @Override
    public String text() {
        return render(value);
    }

    @Override
    public String defaultText() {
        return render(defaultValue());
    }

    private static String render(List<String> list) {
        return list.stream().map(s -> "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
            .collect(Collectors.joining(", ", "[", "]"));
    }
}
