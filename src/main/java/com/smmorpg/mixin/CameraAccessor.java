package com.smmorpg.mixin;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Opens up {@code Camera#move}, which is protected in vanilla. */
@Mixin(Camera.class)
public interface CameraAccessor {
    @Invoker("move")
    void smmorpg$move(double dx, double dy, double dz);
}
