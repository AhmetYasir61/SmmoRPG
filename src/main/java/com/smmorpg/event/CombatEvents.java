package com.smmorpg.event;

import com.smmorpg.SmmoRPG;
import com.smmorpg.core.ModItems;
import com.smmorpg.wound.WoundSystem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Wound upkeep and the two ways to close one by hand.
 *
 * <p>Hitting, blocking, dodging and everything else about the exchange is Epic Fight's;
 * this file is only concerned with what the exchange left behind.
 */
@EventBusSubscriber(modid = SmmoRPG.MOD_ID)
public final class CombatEvents {

    /** Bleeding out and knitting back together, once per entity per tick. */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity e)) return;
        if (e.level().isClientSide) return;
        WoundSystem.tick(e);
    }

    /** Bandage and cautery iron: the two ways to close a wound by hand. */
    @SubscribeEvent
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        if (stack.is(ModItems.BANDAGE.get())) {
            if (WoundSystem.treat(player, 0.55F)) {
                stack.shrink(1);
                event.setCanceled(true);
            }
        } else if (stack.is(ModItems.CAUTERY_IRON.get())) {
            // Searing a wound shut hurts, but it stops the bleeding outright.
            if (WoundSystem.treat(player, 1.0F)) {
                player.hurt(player.damageSources().onFire(), 2.0F);
                stack.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        // A corpse keeps its wounds while it falls, but a respawned player starts clean.
        if (event.getEntity() instanceof Player p && !p.level().isClientSide) {
            WoundSystem.clear(p, event.getSource());
        }
    }
}
