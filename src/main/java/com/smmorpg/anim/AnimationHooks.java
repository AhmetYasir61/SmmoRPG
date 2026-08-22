package com.smmorpg.anim;

import com.smmorpg.SmmoRPG;
import com.smmorpg.core.ModAttachments;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Runs the animation state for every living entity, on both sides.
 *
 * <p>The server needs it as much as the client: the damage window and the weapon's position
 * both come off the animation timeline, so if the server were not running it there would be
 * nothing to check a hit against.
 */
@EventBusSubscriber(modid = SmmoRPG.MOD_ID)
public final class AnimationHooks {

    private AnimationHooks() {}

    public static AnimationState of(LivingEntity entity) {
        return entity.getData(ModAttachments.ANIMATION.get());
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        AnimationState state = of(entity);
        state.tick(entity);

        // Ordinary mobs never call into the moveset themselves, so a vanilla swing is
        // translated into one here. That is what makes every zombie in the world animate,
        // not only the ones the training arena spawned.
        if (!entity.level().isClientSide
                && !(entity instanceof net.minecraft.world.entity.player.Player)
                && entity.swinging && entity.swingTime == 1 && !state.attacking()) {
            var clip = state.attack(false);
            if (clip != null) {
                com.smmorpg.network.Net.broadcastAnimation(entity, clip.id(), 2.0F);
            }
        }
    }
}
