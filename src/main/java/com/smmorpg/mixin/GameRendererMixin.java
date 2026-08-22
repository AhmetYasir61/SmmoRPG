package com.smmorpg.mixin;

import com.smmorpg.client.camera.CameraShake;
import com.smmorpg.client.ClientState;
import com.smmorpg.config.CombatConfig;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppresses the vanilla view-bob while a hit is landing, and hides the floating vanilla
 * hand whenever the real first-person body is being drawn instead.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void smmorpg$suppressBobDuringHitStop(com.mojang.blaze3d.vertex.PoseStack poses,
                                                  float partialTick, CallbackInfo ci) {
        // During hit-stop the world should feel like it stopped, not like it is walking.
        if (CameraShake.inHitStop()) ci.cancel();
        // With a real body on screen, vanilla's bob double-counts the motion.
        else if (CombatConfig.CFG.renderFirstPersonBody.get()
                && ClientState.viewMode.isFirstPerson()) ci.cancel();
    }
}
