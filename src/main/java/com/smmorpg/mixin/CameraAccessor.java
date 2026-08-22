package com.smmorpg.mixin;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Opens up {@code Camera#move}, which is protected in vanilla.
 *
 * <p>The arguments are camera-local floats — forward, up, left — not world-space doubles.
 */
@Mixin(Camera.class)
public interface CameraAccessor {
    @Invoker("move")
    void smmorpg$move(float forward, float up, float left);
}
