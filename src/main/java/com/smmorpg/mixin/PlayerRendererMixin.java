package com.smmorpg.mixin;

import com.smmorpg.client.ClientState;
import com.smmorpg.client.render.PoseState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies the body lean to the third-person rig, from the very same {@link PoseState} the
 * owning client authored for its own first-person view.
 *
 * <p>Only the lean lives here. The limb angles are applied from {@code RenderPlayerEvent.Pre}
 * instead: NeoForge already fires an event at exactly the point a mixin would have injected,
 * and an event that the loader guarantees beats a signature that can drift between versions.
 */
@Mixin(net.minecraft.client.renderer.entity.player.PlayerRenderer.class)
public abstract class PlayerRendererMixin {

    // Full descriptor rather than a bare name: PlayerRenderer carries a synthetic bridge
    // overload of setupRotations taking LivingEntity, and a bare name can match either.
    @Inject(method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;"
                   + "Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V",
            at = @At("RETURN"))
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

}
