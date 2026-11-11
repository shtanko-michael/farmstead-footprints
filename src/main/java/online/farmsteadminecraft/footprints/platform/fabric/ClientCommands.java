package online.farmsteadminecraft.footprints.platform.fabric;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import online.farmsteadminecraft.footprints.client.FootprintsCommands;

/** The client command, run without Fabric API (see ClientPacketListenerMixin). */
public final class ClientCommands {
    private static final String ROOT = "footprints";
    private static final CommandDispatcher<Object> OWN = new CommandDispatcher<>();

    static {
        OWN.register(FootprintsCommands.build());
    }

    private ClientCommands() {
    }

    /** Adds the command to the tree the server sent, so chat suggests it. */
    public static <S> void addTo(CommandDispatcher<S> dispatcher) {
        if (dispatcher.getRoot().getChild(ROOT) == null) dispatcher.register(FootprintsCommands.<S>build());
    }

    /** Runs it if the typed command is ours; false means it belongs to the server. */
    public static boolean run(String command) {
        String root = command.startsWith("/") ? command.substring(1) : command;
        int space = root.indexOf(' ');
        if (!(space < 0 ? root : root.substring(0, space)).equals(ROOT)) return false;
        try {
            OWN.execute(root, new Object());
        } catch (CommandSyntaxException e) {
            Minecraft mc = Minecraft.getInstance();
            Component msg = Component.literal(e.getMessage()).withStyle(ChatFormatting.RED);
            if (mc.player == null) return true;
            //? if >=1.21.11 && <26.1 {
            /*mc.player.displayClientMessage(msg, false);
            *///?} else
            mc.player.sendSystemMessage(msg);
        }
        return true;
    }
}
