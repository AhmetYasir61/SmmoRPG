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
 * model everyone else sees — with the head pushed behind the near plane so it never
 * occludes the view. Because both views read {@link PoseState}, your arms in FPS and your
 * body in someone else's TPS are the same animation, not two approximations of one.
 */
@EventBusSubscriber(modid = SmmoRPG.MOD_ID, value = Dist.CLIENT)
public final class FirstPersonBodyRenderer {

    /** Set while we are drawing the body, so the mixin knows not to hide the player. */
    public static boolean renderingFirstPersonBody = false;

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
            // The head is drawn too, but the camera sits inside it, so only the neck and
            // shoulders ever reach the near plane — which is what selling a real body needs.
            dispatcher.render(player, 0.0D, 0.0D, 0.0D, player.getYRot(), partial, poses, buffers,
                    net.minecraft.client.renderer.LightTexture.pack(15, 15));
        } finally {
            dispatcher.setRenderShadow(true);
            renderingFirstPersonBody = false;
        }

        buffers.endBatch();
        poses.popPose();
    }
}
