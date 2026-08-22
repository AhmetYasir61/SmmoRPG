package com.smmorpg.mixin;

import com.smmorpg.client.ClientState;
import com.smmorpg.client.render.PoseState;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drives the third-person rig from the very same {@link PoseState} the owning client
 * authored for its own first-person view. This is the "everything I see is mirrored on the
 * outside rig" half of the mod.
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {

    @Inject(method = "setupRotations*", at = @At("RETURN"), require = 0)
    private void smmorpg$applyLean(AbstractClientPlayer player, com.mojang.blaze3d.vertex.PoseStack poses,
                                   float ageInTicks, float rotationYaw, float partialTick,
                                   float scale, CallbackInfo ci) {
        PoseState pose = ClientState.pose(player.getId());
        float leanZ = pose.leanZ(partialTick);
        float leanX = pose.leanX(partialTick);
        if (Math.abs(leanZ) > 0.01F) {
            poses.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(Mth.clamp(leanZ, -18.0F, 18.0F)));
        }
        if (Math.abs(leanX) > 0.01F) {
            poses.mulPose(com.mojang.math.Axis.XP.rotationDegrees(Mth.clamp(leanX * 0.35F, -12.0F, 12.0F)));
        }
    }

    @Inject(method = "render*", at = @At("HEAD"), require = 0)
    private void smmorpg$applyAnimation(AbstractClientPlayer player, float entityYaw, float partialTick,
                                        com.mojang.blaze3d.vertex.PoseStack poses,
                                        net.minecraft.client.renderer.MultiBufferSource buffers,
                                        int light, CallbackInfo ci) {
        PoseState pose = ClientState.pose(player.getId());
        PlayerRenderer self = (PlayerRenderer) (Object) this;
        PlayerModel<AbstractClientPlayer> model = self.getModel();

        float t = pose.progress(partialTick);
        // A cut is a fast strike and a slow recovery, so the curve is deliberately skewed.
        float strike = t < 0.35F ? t / 0.35F : 1.0F - (t - 0.35F) / 0.65F;

        switch (pose.animation) {
            case PoseState.ANIM_SLASH_DOWN -> {
                model.rightArm.xRot = -2.6F + strike * 3.4F;
                model.rightArm.zRot = -0.35F + strike * 0.5F;
            }
            case PoseState.ANIM_SLASH_RISING -> {
                model.rightArm.xRot = 0.9F - strike * 3.1F;
                model.rightArm.zRot = 0.4F - strike * 0.7F;
            }
            case PoseState.ANIM_SLASH_HORIZONTAL -> {
                model.rightArm.yRot = -1.5F + strike * 3.0F;
                model.rightArm.xRot = -1.4F;
                model.body.yRot = -0.35F + strike * 0.7F;
            }
            case PoseState.ANIM_THRUST -> {
                model.rightArm.xRot = -1.55F;
                model.rightArm.zRot = -0.1F;
                model.body.yRot = -0.2F * strike;
            }
            case PoseState.ANIM_PARRY -> {
                model.rightArm.xRot = -2.2F;
                model.leftArm.xRot = -1.9F;
                model.rightArm.zRot = 0.6F;
            }
            case PoseState.ANIM_GUARD -> {
                model.rightArm.xRot = -1.7F;
                model.leftArm.xRot = -1.5F;
            }
            case PoseState.ANIM_STAGGER -> {
                model.body.xRot = 0.35F * (1.0F - t);
                model.rightArm.xRot = 0.6F;
                model.leftArm.xRot = 0.6F;
            }
            case PoseState.ANIM_DRAW_BOW -> {
                model.rightArm.xRot = -1.4F;
                model.leftArm.xRot = -1.5F;
                model.rightArm.yRot = -0.5F;
            }
            default -> { }
        }

        // The head follows the real aim pitch rather than the smoothed network value.
        model.head.xRot = Mth.clamp(pose.aimPitch(partialTick) * Mth.DEG_TO_RAD, -1.5F, 1.5F);
    }
}
