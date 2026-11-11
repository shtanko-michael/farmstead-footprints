package online.farmsteadminecraft.footprints.client;

import java.util.List;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Client-side {@code /footprints} command, the same tree on every loader (each loader passes its own
 * command source type): {@code reload} rereads the file, {@code config} opens the settings screen,
 * {@code stats} prints pool usage.
 */
public final class FootprintsCommands {
    private FootprintsCommands() {
    }

    public static <S> LiteralArgumentBuilder<S> build() {
        return LiteralArgumentBuilder.<S>literal("footprints")
            .then(LiteralArgumentBuilder.<S>literal("reload").executes(c -> reload()))
            .then(LiteralArgumentBuilder.<S>literal("config").executes(c -> {
                FootprintsClient.openConfigNextTick();
                return 1;
            }))
            .then(LiteralArgumentBuilder.<S>literal("stats").executes(c -> {
                say(Component.translatable("farmstead_footprints.command.stats", FootprintsClient.POOL.alive(),
                    FootprintsClient.POOL.capacity(), online.farmsteadminecraft.footprints.client.render.FootprintRenderer.lastDrawn));
                return 1;
            }));
    }

    private static int reload() {
        List<String> problems = FootprintsConfig.load();
        if (problems.isEmpty()) {
            say(Component.translatable("farmstead_footprints.command.reloaded").withStyle(ChatFormatting.GREEN));
            return 1;
        }
        say(Component.translatable("farmstead_footprints.command.problems", problems.size()).withStyle(ChatFormatting.GOLD));
        for (String p : problems) say(Component.literal(" - " + p).withStyle(ChatFormatting.GRAY));
        return 0;
    }

    private static void say(Component message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        //? if >=1.21.11 && <26.1 {
        /*mc.player.displayClientMessage(message, false);
        *///?} else
        mc.player.sendSystemMessage(message);
    }
}
