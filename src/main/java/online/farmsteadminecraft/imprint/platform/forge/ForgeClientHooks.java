package online.farmsteadminecraft.imprint.platform.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.fml.ModLoadingContext;
import online.farmsteadminecraft.imprint.client.ImprintCommands;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import online.farmsteadminecraft.imprint.client.ImprintClient;
import online.farmsteadminecraft.imprint.client.render.FootprintRenderer;

final class ForgeClientHooks {
    private ForgeClientHooks() {
    }

    static void register() {
        ImprintClient.init();
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> ImprintClient.configScreen(parent)));
        MinecraftForge.EVENT_BUS.addListener((RegisterClientCommandsEvent event) ->
            event.getDispatcher().register(ImprintCommands.<CommandSourceStack>build()));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) ImprintClient.tick(Minecraft.getInstance());
        });
        MinecraftForge.EVENT_BUS.addListener((RenderLevelStageEvent event) -> {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
                // 1.20.1: the event pose stack carries the camera rotation
                FootprintRenderer.renderImmediate(event.getPoseStack().last().pose(), event.getCamera().getPosition());
            }
        });
    }
}
