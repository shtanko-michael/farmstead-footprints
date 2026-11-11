package online.farmsteadminecraft.imprint.client;

import java.util.List;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Client-side {@code /imprint} command, the same tree on every loader (each loader passes its own
 * command source type): {@code reload} rereads the file, {@code config} opens the settings screen,
 * {@code stats} prints pool usage.
 */
public final class ImprintCommands {
    private ImprintCommands() {
    }

    public static <S> LiteralArgumentBuilder<S> build() {
        return LiteralArgumentBuilder.<S>literal("imprint")
            .then(LiteralArgumentBuilder.<S>literal("reload").executes(c -> reload()))
            .then(LiteralArgumentBuilder.<S>literal("config").executes(c -> {
                ImprintClient.openConfigNextTick();
                return 1;
            }))
            .then(LiteralArgumentBuilder.<S>literal("stats").executes(c -> {
                say(Component.translatable("imprint.command.stats", ImprintClient.POOL.alive(),
                    ImprintClient.POOL.capacity(), online.farmsteadminecraft.imprint.client.render.FootprintRenderer.lastDrawn));
                return 1;
            }));
    }

    private static int reload() {
        List<String> problems = ImprintConfig.load();
        if (problems.isEmpty()) {
            say(Component.translatable("imprint.command.reloaded").withStyle(ChatFormatting.GREEN));
            return 1;
        }
        say(Component.translatable("imprint.command.problems", problems.size()).withStyle(ChatFormatting.GOLD));
        for (String p : problems) say(Component.literal(" - " + p).withStyle(ChatFormatting.GRAY));
        return 0;
    }

    private static void say(Component message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.sendSystemMessage(message);
    }
}
