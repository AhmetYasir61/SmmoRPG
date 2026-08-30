package com.smmorpg.training;

import com.smmorpg.SmmoRPG;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * Two rules that make the arena an arena rather than a field with walls on it.
 *
 * <p>Nothing wanders in. The only things that fight you here are the ones the session
 * chose, so a natural spawn appearing mid-camp — or worse, mid-wave, unscaled and free —
 * is cancelled at the door. A camp you have to defend is not a camp.
 *
 * <p>And the dead pay. Emeralds are the arena's only currency, which is what ties the
 * smith, the craftsman and the merchant to the fighting: everything they do is bought
 * with waves you have already cleared.
 */
@EventBusSubscriber(modid = SmmoRPG.MOD_ID)
public final class ArenaRules {

    private ArenaRules() {}

    @SubscribeEvent
    public static void onNaturalSpawn(FinalizeSpawnEvent event) {
        MobSpawnType type = event.getSpawnType();
        if (type != MobSpawnType.NATURAL && type != MobSpawnType.CHUNK_GENERATION
                && type != MobSpawnType.STRUCTURE && type != MobSpawnType.PATROL) {
            return;
        }

        if (event.getLevel() instanceof ServerLevel world && TrainingArena.isArenaWorld(world)) {
            event.setSpawnCancelled(true);
        }
    }

    /**
     * What an arena kill is worth.
     *
     * <p>Scaled by the level it died at, so climbing pays for the repairs the climb costs
     * you. Killing things outside a session pays nothing — this is arena money, and a
     * player farming cows for shop stock would be playing a different game.
     */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        TrainingSession session = TrainingManager.of(player);
        if (session == null) return;

        int level = session.level();
        int payout = 1 + player.getRandom().nextInt(2 + level / 2);

        drop(event.getEntity(), payout);
    }

    private static void drop(Entity dead, int emeralds) {
        while (emeralds > 0) {
            int stack = Math.min(64, emeralds);
            emeralds -= stack;
            dead.spawnAtLocation(new ItemStack(Items.EMERALD, stack));
        }
    }
}
