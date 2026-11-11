package online.farmsteadminecraft.imprint.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Settings screen generated from a {@link ConfigFile}: a preset button on top, then one row per
 * value (label on the left, control on the right) split into pages. Built only from widgets that
 * exist unchanged from 1.20.1 to 26.x (Button, AbstractSliderButton, EditBox, StringWidget), and
 * it never overrides render, so the same class works on every version we ship.
 *
 * <p>Changes apply to the live values at once; "Done" saves the file, "Cancel" reloads it.
 */
public class ConfigScreen extends Screen {
    private static final int ROW = 24;
    private static final int CONTROL_W = 160;

    private final Screen parent;
    private final ConfigFile file;
    private final Runnable onSave;
    private final Runnable onCancel;
    private int page;

    public ConfigScreen(Screen parent, ConfigFile file, Runnable onSave, Runnable onCancel) {
        super(Component.translatableWithFallback("config." + file.modId() + ".title", file.modId() + " settings"));
        this.parent = parent;
        this.file = file;
        this.onSave = onSave;
        this.onCancel = onCancel;
    }

    private int rowsPerPage() {
        return Math.max(3, (height - 32 - 30 - 40) / ROW);
    }

    private int pages() {
        return Math.max(1, (file.values().size() + rowsPerPage() - 1) / rowsPerPage());
    }

    @Override
    protected void init() {
        page = Math.min(page, pages() - 1);
        int center = width / 2;
        addRenderableWidget(new StringWidget(0, 8, width, 12, title, font));

        Button preset = Button.builder(presetLabel(), b -> {
            List<String> names = new ArrayList<>(file.presetNames());
            String next = names.get((names.indexOf(file.preset()) + 1) % names.size());
            file.applyPreset(next);
            rebuildWidgets();
        }).bounds(center - 100, 24, 200, 20).build();
        preset.setTooltip(Tooltip.create(text("preset.tooltip", "Sets every value below at once. Values you change afterwards are kept.")));
        addRenderableWidget(preset);

        int top = 54;
        int labelW = Math.min(220, width / 2 - 20);
        int labelX = center - labelW - 10;
        int controlX = center + 10;
        List<ConfigValue<?>> values = file.values();
        int from = page * rowsPerPage();
        int to = Math.min(values.size(), from + rowsPerPage());
        for (int i = from; i < to; i++) {
            ConfigValue<?> v = values.get(i);
            int y = top + (i - from) * ROW;
            StringWidget label = new StringWidget(labelX, y + 5, labelW, 12, name(v), font);
            addRenderableWidget(label);
            AbstractWidget control = control(v, controlX, y);
            control.setTooltip(Tooltip.create(tooltip(v)));
            addRenderableWidget(control);
        }

        int by = height - 28;
        if (pages() > 1) {
            Button prev = Button.builder(Component.literal("<"), b -> { page--; rebuildWidgets(); })
                .bounds(center - 154, by, 20, 20).build();
            prev.active = page > 0;
            addRenderableWidget(prev);
            Button next = Button.builder(Component.literal(">"), b -> { page++; rebuildWidgets(); })
                .bounds(center + 134, by, 20, 20).build();
            next.active = page < pages() - 1;
            addRenderableWidget(next);
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> {
            onSave.run();
            close();
        }).bounds(center - 130, by, 128, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> {
            onCancel.run();
            close();
        }).bounds(center + 2, by, 128, 20).build());
    }

    private void close() {
        if (minecraft == null) return;
        //? if >=26.1 {
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

    private AbstractWidget control(ConfigValue<?> v, int x, int y) {
        if (v instanceof BoolValue b) {
            return cycle(x, y, () -> onOff(b.on()), () -> b.set(!b.on()));
        }
        if (v instanceof EnumValue<?> e) {
            return cycle(x, y, () -> enumLabel(e), () -> cycleEnum(e));
        }
        if (v instanceof IntValue i) {
            return new Slider(x, y, (i.value() - i.min) / (double) (i.max - i.min),
                s -> i.set((int) Math.round(i.min + s * (i.max - i.min))), () -> Component.literal(String.valueOf(i.value())));
        }
        if (v instanceof DoubleValue d) {
            return new Slider(x, y, (d.value() - d.min) / (d.max - d.min),
                s -> d.set(Math.round((d.min + s * (d.max - d.min)) * 100) / 100.0),
                () -> Component.literal(DoubleValue.fmt(d.value())));
        }
        if (v instanceof StringListValue l) {
            EditBox box = new EditBox(font, x, y, CONTROL_W, 20, name(v));
            box.setMaxLength(2048);
            box.setValue(String.join(", ", l.get()));
            box.setResponder(s -> l.set(Arrays.stream(s.split(",")).map(String::trim).filter(t -> !t.isEmpty()).toList()));
            return box;
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

    private Button cycle(int x, int y, java.util.function.Supplier<Component> label, Runnable advance) {
        Button[] self = new Button[1];
        self[0] = Button.builder(label.get(), b -> {
            advance.run();
            self[0].setMessage(label.get());
        }).bounds(x, y, CONTROL_W, 20).build();
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

    private static final class Slider extends AbstractSliderButton {
        private final Consumer<Double> apply;
        private final java.util.function.Supplier<Component> label;

        Slider(int x, int y, double initial, Consumer<Double> apply, java.util.function.Supplier<Component> label) {
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
