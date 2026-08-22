package com.smmorpg.event;

import com.smmorpg.SmmoRPG;
import com.smmorpg.capability.PlayerProgress;
import com.smmorpg.combat.CombatEngine;
import com.smmorpg.combat.CombatState;
import com.smmorpg.core.ModAttachments;
import com.smmorpg.core.ModAttributes;
import com.smmorpg.core.ModItems;
import com.smmorpg.item.RpgWeaponItem;
import com.smmorpg.item.WeaponClass;
import com.smmorpg.wound.WoundSystem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Wires vanilla combat events into {@link CombatEngine} and the wound system. */
@EventBusSubscriber(modid = SmmoRPG.MOD_ID)
public final class CombatEvents {

    /**
     * Runs at LOWEST so every other mod's damage modification is already in the number we
     * hand to the engine. The engine then applies locational and gear scaling and rewrites it.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        // Only direct blows get the locational treatment; fire and fall damage stay vanilla.
        if (event.getSource().getDirectEntity() != event.getSource().getEntity()) return;

        CombatEngine.Blow blow = CombatEngine.strike(attacker, target, event.getAmount());

        if (blow.parried()) {
            event.setCanceled(true);
            return;
        }
        event.setAmount(blow.damage());
    }

    /** Swinging costs stamina; swinging on empty is a feeble hit. */
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        CombatState state = player.getData(ModAttachments.COMBAT.get());
        WeaponClass wc = RpgWeaponItem.classOf(player.getMainHandItem());
        float cost = wc == null ? 12.0F : wc.staminaCost();

        if (!state.spendStamina(cost)) {
            // Not enough left in the tank: the swing goes through, but weakly.
            state.stamina = 0.0F;
        }
    }

    /** Right-clicking with a weapon opens the parry window instead of a vanilla block. */
    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickItem event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getItemStack().getItem() instanceof RpgWeaponItem)) return;

        Player player = event.getEntity();
        CombatState state = player.getData(ModAttachments.COMBAT.get());
        if (state.staggered()) return;
        PlayerProgress progress = player.getData(ModAttachments.PROGRESS.get());
        state.openParryWindow(progress.playerClass().parryWindowTicks());
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity e)) return;
        if (e.level().isClientSide) return;

        CombatState state = e.getData(ModAttachments.COMBAT.get());
        float regen = 1.2F;
        if (e instanceof Player p) {
            PlayerProgress progress = p.getData(ModAttachments.PROGRESS.get());
            state.staminaMax = progress.staminaMax();
            regen = progress.staminaRegen();
        }
        state.postureMax = (float) e.getAttributeValue(ModAttributes.POSTURE.getDelegate());
        state.tick(regen, 8.0F);

        com.smmorpg.movement.MovementSystem.tick(e);
        com.smmorpg.combat.SwingTracker.tick(e);
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
