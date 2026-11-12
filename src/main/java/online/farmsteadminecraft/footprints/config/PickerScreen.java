package online.farmsteadminecraft.footprints.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Picks block or mob ids for a list setting: a search box (by name in the game's language or by id), the matches as
 * toggle buttons, paged. Entries already in the list come first, tags included; anything typed (a #tag, a modded id
 * not loaded right now) can be added as it is. Widgets only, like {@link ConfigScreen}.
 */
public class PickerScreen extends Screen {
    private static final int ROW = 22;

    private final ConfigScreen parent;
    private final StringListValue list;
    private final String modId;
    private final List<Entry> all = new ArrayList<>();
    private EditBox search;
    private String query = "";
    private int page;

    private record Entry(String id, Component name, String haystack) {}

    PickerScreen(ConfigScreen parent, StringListValue list, Component title, String modId) {
        super(title);
        this.parent = parent;
        this.list = list;
        this.modId = modId;
        if (list.picker() == StringListValue.Picker.BLOCKS) {
            for (ResourceLocation id : BuiltInRegistries.BLOCK.keySet()) {
                //? if >=1.21.2 {
                /*Component name = BuiltInRegistries.BLOCK.getValue(id).getName();
                *///?} else
                Component name = BuiltInRegistries.BLOCK.get(id).getName();
                all.add(new Entry(id.toString(), name, (id + " " + name.getString()).toLowerCase(Locale.ROOT)));
            }
        } else {
            for (ResourceLocation id : BuiltInRegistries.ENTITY_TYPE.keySet()) {
                //? if >=1.21.2 {
                /*Component name = BuiltInRegistries.ENTITY_TYPE.getValue(id).getDescription();
                *///?} else
                Component name = BuiltInRegistries.ENTITY_TYPE.get(id).getDescription();
                all.add(new Entry(id.toString(), name, (id + " " + name.getString()).toLowerCase(Locale.ROOT)));
            }
        }
        all.sort((a, b) -> a.id().compareTo(b.id()));
    }

    /** Types a search, as if the player did (test hooks). */
    public void search(String text) {
        preset(text);
        page = 0;
        rebuildWidgets();
    }

    /** Starts with this search typed in. */
    void preset(String text) {
        query = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }

    private int rows() {
        return Math.max(4, (height - 64 - 36) / ROW);
    }

    private List<Entry> matches() {
        List<Entry> out = new ArrayList<>();
        // what is already chosen comes first, whatever it is (tags, ids of mods not installed)
        for (String chosen : list.get()) {
            Entry known = all.stream().filter(e -> e.id().equals(chosen)).findFirst().orElse(null);
            Entry e = known != null ? known : new Entry(chosen, Component.literal(chosen), chosen.toLowerCase(Locale.ROOT));
            if (query.isEmpty() || e.haystack().contains(query)) out.add(e);
        }
        for (Entry e : all) {
            if (list.contains(e.id())) continue;
            if (query.isEmpty() || e.haystack().contains(query)) out.add(e);
        }
        return out;
    }

    @Override
    protected void init() {
        int center = width / 2;
        addRenderableWidget(new StringWidget((width - Math.min(width, font.width(title))) / 2, 8, Math.min(width, font.width(title)), 12, title, font));
        search = new EditBox(font, center - 150, 26, 220, 20, Component.literal("search"));
        search.setValue(query);
        search.setResponder(s -> {
            query = s.trim().toLowerCase(Locale.ROOT);
            page = 0;
            rebuildWidgets();
            setFocused(search);
            search.setFocused(true);
        });
        addRenderableWidget(search);
        Button add = Button.builder(text("picker.add", "Add as typed"), b -> {
            String raw = search.getValue().trim();
            if (raw.isEmpty() || list.contains(raw)) return;
            List<String> next = new ArrayList<>(list.get());
            next.add(raw);
            list.set(next);
            rebuildWidgets();
        }).bounds(center + 74, 26, 76, 20).build();
        add.setTooltip(Tooltip.create(text("picker.add.tooltip", "Adds exactly what is typed, for a #tag or an id from a mod that is not loaded.")));
        addRenderableWidget(add);

        List<Entry> found = matches();
        int pages = Math.max(1, (found.size() + rows() * 2 - 1) / (rows() * 2));
        page = Math.min(page, pages - 1);
        int from = page * rows() * 2;
        for (int i = 0; i < rows() * 2 && from + i < found.size(); i++) {
            Entry e = found.get(from + i);
            int col = i / rows(), row = i % rows();
            boolean on = list.contains(e.id());
            Button b = Button.builder(label(e, on), btn -> {
                List<String> next = new ArrayList<>(list.get());
                if (!next.remove(e.id())) next.add(e.id());
                list.set(next);
                rebuildWidgets();
            }).bounds(center - 150 + col * 152, 56 + row * ROW, 148, 20).build();
            b.setTooltip(Tooltip.create(Component.literal(e.id())));
            addRenderableWidget(b);
        }
        int by = height - 28;
        Button prev = Button.builder(Component.literal("<"), b -> { page--; rebuildWidgets(); }).bounds(center - 150, by, 20, 20).build();
        prev.active = page > 0;
        addRenderableWidget(prev);
        Button next = Button.builder(Component.literal(">"), b -> { page++; rebuildWidgets(); }).bounds(center + 130, by, 20, 20).build();
        next.active = page < pages - 1;
        addRenderableWidget(next);
        addRenderableWidget(new StringWidget(center - 126, by + 5, 60, 12, Component.literal((page + 1) + " / " + pages), font));
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> parent.reopen()).bounds(center - 60, by, 120, 20).build());
    }

    private Component label(Entry e, boolean on) {
        String name = e.name().getString();
        if (name.length() > 20) name = name.substring(0, 19) + "…";
        return Component.literal((on ? "§a✔ §r" : "§7○ §r") + name);
    }

    private Component text(String key, String fallback) {
        return Component.translatableWithFallback("config." + modId + "." + key, fallback);
    }

    @Override
    public void onClose() {
        parent.reopen();
    }
}
