package com.smmorpg.mixin;

import com.smmorpg.client.camera.CameraShake;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the positional half of the shake. Rotation alone reads as mouse jitter; a real blow
 * also moves the head, and that displacement is what makes the hit land in the body.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {

    @Inject(method = "setup", at = @At("RETURN"))
    private void smmorpg$applyPositionalShake(net.minecraft.world.level.BlockGetter level,
                                              net.minecraft.world.entity.Entity entity,
                                              boolean detached, boolean mirrored, float partialTick,
                                              CallbackInfo ci) {
        float time = (System.nanoTime() % 1_000_000_000_000L) / 1.0E9F;
        float offset = CameraShake.positionalOffset(time);
        if (Math.abs(offset) < 1.0E-4F) return;
        ((CameraAccessor) this).smmorpg$move(offset * 0.6F, offset, offset * 0.4F);
    }
}
