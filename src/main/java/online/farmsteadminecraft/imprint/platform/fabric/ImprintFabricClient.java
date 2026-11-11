package online.farmsteadminecraft.imprint.platform.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import online.farmsteadminecraft.imprint.client.ImprintClient;
import online.farmsteadminecraft.imprint.client.render.FootprintRenderer;
//? if >=26.1 {
/*import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
*///?} else {
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
//?}

public class ImprintFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(ImprintClient::tick);
        //? if >=26.1 {
        /*LevelRenderEvents.COLLECT_SUBMITS.register(ctx ->
            FootprintRenderer.submit(ctx.submitNodeCollector(), ctx.poseStack(), ctx.levelState().cameraRenderState.pos));
        *///?} else {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(ctx ->
            FootprintRenderer.renderImmediate(viewMatrix(ctx), ctx.camera().getPosition()));
        //?}
    }

    //? if <26.1 {
    private static org.joml.Matrix4f viewMatrix(WorldRenderContext ctx) {
        return ctx.matrixStack().last().pose();
    }
    //?}
}
