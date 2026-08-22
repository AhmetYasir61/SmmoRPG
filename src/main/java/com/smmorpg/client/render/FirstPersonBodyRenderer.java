package com.smmorpg.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.smmorpg.SmmoRPG;
import com.smmorpg.client.ClientState;
import com.smmorpg.config.CombatConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Renders the local player's real body while in first person.
 *
 * <p>Vanilla draws a floating pair of hands. This draws the actual player model — the same
 * model everyone else sees. The head is stripped out for this pass by
 * {@code ClientRenderEvents}, because the camera is inside it and drawing it would fill the
 * screen with the back of your own skull. Because both views read {@link PoseState}, your arms in FPS and your
 * body in someone else's TPS are the same animation, not two approximations of one.
 */
@EventBusSubscriber(modid = SmmoRPG.MOD_ID, value = Dist.CLIENT)
public final class FirstPersonBodyRenderer {

    /** Set while we are drawing the body, so the mixin knows not to hide the player. */
    public static boolean renderingFirstPersonBody = false;

    /**
     * Hides vanilla's floating hand while the real body is on screen.
     *
     * <p>The body already carries the held item through its own in-hand layer, so leaving
     * vanilla's hand renderer running would draw the weapon twice, in two places, moving to
     * two different animations.
     */
    @SubscribeEvent
    public static void onRenderHand(net.neoforged.neoforge.client.event.RenderHandEvent event) {
        if (!CombatConfig.CFG.renderFirstPersonBody.get()) return;
        if (!ClientState.viewMode.isFirstPerson()) return;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        if (!CombatConfig.CFG.renderFirstPersonBody.get()) return;
        if (!ClientState.viewMode.isFirstPerson()) return;

        Minecraft mc = Minecraft.getInstance();
        AbstractClientPlayer player = mc.player;
        if (player == null || mc.options.hideGui) return;
        if (mc.gameRenderer.getMainCamera().isDetached()) return;

        PoseStack poses = event.getPoseStack();
        var camera = mc.gameRenderer.getMainCamera();
        var camPos = camera.getPosition();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);

        double x = net.minecraft.util.Mth.lerp(partial, player.xo, player.getX()) - camPos.x;
        double y = net.minecraft.util.Mth.lerp(partial, player.yo, player.getY()) - camPos.y;
        double z = net.minecraft.util.Mth.lerp(partial, player.zo, player.getZ()) - camPos.z;

        poses.pushPose();
        poses.translate(x, y, z);

        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        renderingFirstPersonBody = true;
        dispatcher.setRenderShadow(false);
        try {
            // Real light, not full-bright: a body lit differently from the world it stands
            // in reads as a decal pasted over the screen.
            int light = dispatcher.getPackedLightCoords(player, partial);
            dispatcher.render(player, 0.0D, 0.0D, 0.0D, player.getYRot(), partial, poses, buffers,
                    light);
        } finally {
            dispatcher.setRenderShadow(true);
            renderingFirstPersonBody = false;
            if (dispatcher.getRenderer(player) instanceof
                    net.minecraft.client.renderer.entity.LivingEntityRenderer<?, ?> living
                    && living.getModel() instanceof net.minecraft.client.model.PlayerModel<?> model) {
                ClientRenderEvents.restoreHeadIfHidden(model);
            }
        }

        buffers.endBatch();
        poses.popPose();
    }
}
