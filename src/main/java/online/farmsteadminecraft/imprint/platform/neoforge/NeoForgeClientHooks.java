package online.farmsteadminecraft.imprint.platform.neoforge;

import net.minecraft.client.Minecraft;
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

    static void register() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> ImprintClient.tick(Minecraft.getInstance()));
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
