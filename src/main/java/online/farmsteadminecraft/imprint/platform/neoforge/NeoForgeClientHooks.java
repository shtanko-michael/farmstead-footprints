package online.farmsteadminecraft.imprint.platform.neoforge;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import online.farmsteadminecraft.imprint.client.ImprintCommands;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import online.farmsteadminecraft.imprint.client.ImprintClient;
import online.farmsteadminecraft.imprint.client.render.FootprintRenderer;
//? if >=26.1 {
/*import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
*///?} else {
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
//?}

final class NeoForgeClientHooks {
    private NeoForgeClientHooks() {
    }

    static void register(ModContainer container) {
        ImprintClient.init();
        container.registerExtensionPoint(IConfigScreenFactory.class,
            (IConfigScreenFactory) (mod, parent) -> ImprintClient.configScreen(parent));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> ImprintClient.tick(Minecraft.getInstance()));
        NeoForge.EVENT_BUS.addListener((RegisterClientCommandsEvent event) ->
            event.getDispatcher().register(ImprintCommands.<CommandSourceStack>build()));
        //? if >=26.1 {
        /*NeoForge.EVENT_BUS.addListener((SubmitCustomGeometryEvent event) ->
            FootprintRenderer.submit(event.getSubmitNodeCollector(), event.getPoseStack(),
                event.getLevelRenderState().cameraRenderState.pos));
        *///?} else {
        NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent event) -> {
            // After particles the global model-view matrix carries the camera rotation, so the
            // event's identity pose stack plus camera-relative vertices land in the right place
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
                FootprintRenderer.renderImmediate(event.getPoseStack().last().pose(), event.getCamera().getPosition());
            }
        });
        //?}
    }
}
