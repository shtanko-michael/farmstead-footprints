package online.farmsteadminecraft.footprints.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Settings screen generated from a {@link ConfigFile}: one tab per section of the file plus an "About" tab, a preset
 * button, then one row per value (label on the left, control on the right), paged when a tab is long. Lists of block
 * or mob ids get a searchable picker ({@link PickerScreen}). Built only from widgets that exist unchanged from 1.20.1
 * to 26.x (Button, AbstractSliderButton, EditBox, StringWidget), and it never overrides render, so the same class
 * works on every version we ship.
 *
 * <p>Changes apply to the live values at once; "Done" saves the file, "Cancel" reloads it. "Try it" (when the mod
 * offers one) closes the screen and shows the current settings in the world.</p>
 */
public class ConfigScreen extends Screen {
    private static final int ROW = 24;
    private static final int CONTROL_W = 160;
    private static final String ABOUT = "about";

    /** What the About tab and the bottom row show besides the values. */
    public record Extras(String name, String version, Supplier<String> latest, Supplier<String> source, List<Link> links, Runnable tryIt) {}

    public record Link(String key, String fallback, String url) {}

    private final Screen parent;
    private final ConfigFile file;
    private final Runnable onSave;
    private final Runnable onCancel;
    private final Extras extras;
    private String tab;
    private int page;

    public ConfigScreen(Screen parent, ConfigFile file, Runnable onSave, Runnable onCancel) {
        this(parent, file, onSave, onCancel, null);
    }

    public ConfigScreen(Screen parent, ConfigFile file, Runnable onSave, Runnable onCancel, Extras extras) {
        super(Component.translatableWithFallback("config." + file.modId() + ".title", file.modId() + " settings"));
        this.parent = parent;
        this.file = file;
        this.onSave = onSave;
        this.onCancel = onCancel;
        this.extras = extras;
    }

    private Map<String, List<ConfigValue<?>>> sections() {
        Map<String, List<ConfigValue<?>>> out = new LinkedHashMap<>();
        for (ConfigValue<?> v : file.values()) out.computeIfAbsent(v.section(), s -> new ArrayList<>()).add(v);
        return out;
    }

    private int rowsPerPage() {
        return Math.max(3, (height - 76 - 40) / ROW);
    }

    @Override
    protected void init() {
        Map<String, List<ConfigValue<?>>> sections = sections();
        List<String> tabs = new ArrayList<>(sections.keySet());
        if (extras != null) tabs.add(ABOUT);
        if (tab == null || !tabs.contains(tab)) tab = tabs.get(0);
        int center = width / 2;
        addRenderableWidget(centered(0, 6, width, title));

        // tabs
        int tabW = Math.min(110, (width - 20) / tabs.size());
        int tabX = center - tabW * tabs.size() / 2;
        for (int i = 0; i < tabs.size(); i++) {
            String t = tabs.get(i);
            Button b = Button.builder(text("section." + t, pretty(t)), btn -> {
                tab = t;
                page = 0;
                rebuildWidgets();
            }).bounds(tabX + i * tabW, 20, tabW - 2, 20).build();
            b.active = !t.equals(tab);
            addRenderableWidget(b);
        }

        int by = height - 28;
        if (ABOUT.equals(tab)) {
            about(center);
        } else {
            Button preset = Button.builder(presetLabel(), b -> {
                List<String> names = new ArrayList<>(file.presetNames());
                String next = names.get((names.indexOf(file.preset()) + 1) % names.size());
                file.applyPreset(next);
                rebuildWidgets();
            }).bounds(center - 100, 46, 200, 20).build();
            preset.setTooltip(Tooltip.create(text("preset.tooltip", "Sets every value below at once. Values you change afterwards are kept.")));
            addRenderableWidget(preset);

            List<ConfigValue<?>> values = sections.get(tab);
            int pages = Math.max(1, (values.size() + rowsPerPage() - 1) / rowsPerPage());
            page = Math.min(page, pages - 1);
            int top = 76;
            int labelW = Math.min(220, width / 2 - 20);
            int labelX = center - labelW - 10;
            int controlX = center + 10;
            int from = page * rowsPerPage();
            int to = Math.min(values.size(), from + rowsPerPage());
            for (int i = from; i < to; i++) {
                ConfigValue<?> v = values.get(i);
                int y = top + (i - from) * ROW;
                addRenderableWidget(centered(labelX, y + 5, labelW, name(v)));
                for (AbstractWidget control : controls(v, controlX, y)) {
                    control.setTooltip(Tooltip.create(tooltip(v)));
                    addRenderableWidget(control);
                }
            }
            if (pages > 1) {
                Button prev = Button.builder(Component.literal("<"), b -> { page--; rebuildWidgets(); })
                    .bounds(center - 198, by, 20, 20).build();
                prev.active = page > 0;
                addRenderableWidget(prev);
                Button next = Button.builder(Component.literal(">"), b -> { page++; rebuildWidgets(); })
                    .bounds(center + 178, by, 20, 20).build();
                next.active = page < pages - 1;
                addRenderableWidget(next);
            }
        }

        boolean tryIt = extras != null && extras.tryIt() != null;
        int w = tryIt ? 116 : 128, x = tryIt ? center - 176 : center - 130;
        if (tryIt) {
            Button t = Button.builder(text("try", "Try it"), b -> {
                close();
                extras.tryIt().run();
            }).bounds(x, by, w, 20).build();
            t.setTooltip(Tooltip.create(text("try.tooltip", "Closes this screen and shows the current settings on the ground where you look.")));
            addRenderableWidget(t);
            x += w + 4;
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> {
            onSave.run();
            close();
        }).bounds(x, by, w, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> {
            onCancel.run();
            close();
        }).bounds(x + w + 4, by, w, 20).build());
    }

    private void about(int center) {
        int y = 54;
        addRenderableWidget(centered(0, y, width, Component.literal(extras.name() + " " + extras.version())));
        String latest = extras.latest().get();
        Component status = latest == null
            ? text("about.unknown", "Update check: not done (switched off, or no answer from Modrinth or CurseForge)")
            : latest.isEmpty()
                ? text("about.current", "You have the latest version.")
                : Component.translatableWithFallback("config." + file.modId() + ".about.newer", "A newer version is out: %s", latest);
        addRenderableWidget(centered(0, y + 16, width, status));
        String from = extras.source().get();
        if (latest != null && from != null) {
            addRenderableWidget(centered(0, y + 28, width,
                Component.translatableWithFallback("config." + file.modId() + ".about.source", "Checked on %s", from)));
            y += 12;
        }
        y += 40;
        for (Link link : extras.links()) {
            addRenderableWidget(Button.builder(text("about." + link.key(), link.fallback()), b -> openLink(link.url()))
                .bounds(center - 100, y, 200, 20).build());
            y += 24;
        }
    }

    private void openLink(String url) {
        //? if >=26.2 {
        /*ConfirmLinkScreen.confirmLinkNow(this, java.net.URI.create(url));
        *///?} else if >=1.21 {
        ConfirmLinkScreen.confirmLinkNow(this, url);
        //?} else
        /*ConfirmLinkScreen.confirmLinkNow(url, this, true);*/
    }

    /** Switches to a tab (a section name or "about"); for test hooks and links into the settings. */
    public void showTab(String name) {
        tab = name;
        page = 0;
        rebuildWidgets();
    }

    /** Opens the picker of a list value by its path ("surfaces.include"); for test hooks. */
    public void openPicker(String path, String query) {
        if (!(file.find(path) instanceof StringListValue l) || l.picker() == null || minecraft == null) return;
        PickerScreen screen = new PickerScreen(this, l, name(l), file.modId());
        screen.preset(query);
        //? if >=26.2 {
        /*minecraft.gui.setScreen(screen);
        *///?} else
        minecraft.setScreen(screen);
    }

    void reopen() {
        if (minecraft == null) return;
        //? if >=26.2 {
        /*minecraft.gui.setScreen(this);
        *///?} else
        minecraft.setScreen(this);
    }

    private void close() {
        if (minecraft == null) return;
        //? if >=26.2 {
        /*minecraft.gui.setScreen(parent);
        *///?} else
        minecraft.setScreen(parent);
    }

    @Override
    public void onClose() {
        onCancel.run();
        close();
    }

    private Component presetLabel() {
        return text("preset", "Preset").copy().append(": ").append(text("preset." + file.preset(), pretty(file.preset())));
    }

    private List<AbstractWidget> controls(ConfigValue<?> v, int x, int y) {
        if (v instanceof BoolValue b) {
            return List.of(cycle(x, y, CONTROL_W, () -> onOff(b.on()), () -> b.set(!b.on())));
        }
        if (v instanceof EnumValue<?> e) {
            return List.of(cycle(x, y, CONTROL_W, () -> enumLabel(e), () -> cycleEnum(e)));
        }
        if (v instanceof IntValue i) {
            return List.of(new Slider(x, y, (i.value() - i.min) / (double) (i.max - i.min),
                s -> i.set((int) Math.round(i.min + s * (i.max - i.min))), () -> Component.literal(String.valueOf(i.value()))));
        }
        if (v instanceof DoubleValue d) {
            return List.of(new Slider(x, y, (d.value() - d.min) / (d.max - d.min),
                s -> d.set(Math.round((d.min + s * (d.max - d.min)) * 100) / 100.0),
                () -> Component.literal(DoubleValue.fmt(d.value()))));
        }
        if (v instanceof StringListValue l) {
            boolean picker = l.picker() != null;
            EditBox box = new EditBox(font, x, y, picker ? CONTROL_W - 24 : CONTROL_W, 20, name(v));
            box.setMaxLength(4096);
            box.setValue(String.join(", ", l.get()));
            box.setResponder(s -> l.set(Arrays.stream(s.split(",")).map(String::trim).filter(t -> !t.isEmpty()).toList()));
            if (!picker) return List.of(box);
            Button pick = Button.builder(Component.literal("..."), b -> {
                if (minecraft == null) return;
                PickerScreen screen = new PickerScreen(this, l, name(v), file.modId());
                //? if >=26.2 {
                /*minecraft.gui.setScreen(screen);
                *///?} else
                minecraft.setScreen(screen);
            }).bounds(x + CONTROL_W - 20, y, 20, 20).build();
            return List.of(box, pick);
        }
        throw new IllegalStateException("no control for " + v.getClass().getSimpleName());
    }

    private static <E extends Enum<E>> void cycleEnum(EnumValue<E> e) {
        e.set(e.next());
    }

    private Component enumLabel(EnumValue<?> e) {
        String name = e.get().name().toLowerCase(Locale.ROOT);
        return text(e.path() + "." + name, pretty(name));
    }

    private Component onOff(boolean on) {
        return Component.translatable(on ? "options.on" : "options.off");
    }

    private Button cycle(int x, int y, int w, Supplier<Component> label, Runnable advance) {
        Button[] self = new Button[1];
        self[0] = Button.builder(label.get(), b -> {
            advance.run();
            self[0].setMessage(label.get());
        }).bounds(x, y, w, 20).build();
        return self[0];
    }

    private Component name(ConfigValue<?> v) {
        return Component.translatableWithFallback(v.translationKey(), pretty(v.key()));
    }

    private Component tooltip(ConfigValue<?> v) {
        String extra = v.rangeText().isEmpty() ? "" : "\n" + v.rangeText();
        return Component.translatableWithFallback(v.translationKey() + ".tooltip", v.comment())
            .copy().append(Component.literal(extra + "\n" + "default: " + v.defaultText()));
    }

    private Component text(String key, String fallback) {
        return Component.translatableWithFallback("config." + file.modId() + "." + key, fallback);
    }

    /** {@code renderDistance} to "Render distance", {@code vanilla_plus} to "Vanilla plus". */
    static String pretty(String key) {
        String spaced = key.replace('_', ' ').replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase(Locale.ROOT);
        return spaced.isEmpty() ? key : Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    /** Text centred in a box: 26.x StringWidget draws left-aligned, so the widget is sized to the text. */
    private StringWidget centered(int x, int y, int w, Component text) {
        int tw = Math.min(w, font.width(text));
        return new StringWidget(x + (w - tw) / 2, y, tw, 12, text, font);
    }

    private static final class Slider extends AbstractSliderButton {
        private final Consumer<Double> apply;
        private final Supplier<Component> label;

        Slider(int x, int y, double initial, Consumer<Double> apply, Supplier<Component> label) {
            super(x, y, CONTROL_W, 20, label.get(), Math.max(0, Math.min(1, initial)));
            this.apply = apply;
            this.label = label;
        }

        @Override
        protected void updateMessage() {
            setMessage(label.get());
        }

        @Override
        protected void applyValue() {
            apply.accept(value);
        }
    }
}
