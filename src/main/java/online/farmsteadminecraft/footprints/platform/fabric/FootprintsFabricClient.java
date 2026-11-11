package online.farmsteadminecraft.footprints.platform.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import online.farmsteadminecraft.footprints.client.FootprintsCommands;
import online.farmsteadminecraft.footprints.client.FootprintsClient;
import online.farmsteadminecraft.footprints.client.render.FootprintRenderer;
//? if >=26.1 {
/*import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
*///?} else {
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
//?}

public class FootprintsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FootprintsClient.init();
        ClientTickEvents.END_CLIENT_TICK.register(FootprintsClient::tick);
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) ->
            dispatcher.register(FootprintsCommands.<FabricClientCommandSource>build()));
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
