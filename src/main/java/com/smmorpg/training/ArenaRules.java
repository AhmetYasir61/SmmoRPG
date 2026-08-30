package com.smmorpg.training;

import com.smmorpg.SmmoRPG;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
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

    /** Just past the 32x32 floor, so the exclusion covers the walls too. */
    private static final double RADIUS_SQR = 24.0D * 24.0D;

    private ArenaRules() {}

    @SubscribeEvent
    public static void onNaturalSpawn(FinalizeSpawnEvent event) {
        MobSpawnType type = event.getSpawnType();
        if (type != MobSpawnType.NATURAL && type != MobSpawnType.CHUNK_GENERATION
                && type != MobSpawnType.STRUCTURE && type != MobSpawnType.PATROL) {
            return;
        }

        if (insideAnyArena(event.getEntity().position())) {
            event.setSpawnCancelled(true);
        }
    }

    private static boolean insideAnyArena(Vec3 pos) {
        for (TrainingSession session : TrainingManager.sessions()) {
            if (session.centre().distanceToSqr(pos) < RADIUS_SQR) return true;
        }
        return false;
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

        LivingEntity dead = event.getEntity();
        if (!insideAnyArena(dead.position())) return;

        int level = session.level();
        int payout = 1 + player.getRandom().nextInt(2 + level / 2);

        drop(dead, payout);
    }

    private static void drop(Entity dead, int emeralds) {
        while (emeralds > 0) {
            int stack = Math.min(64, emeralds);
            emeralds -= stack;
            dead.spawnAtLocation(new ItemStack(Items.EMERALD, stack));
        }
    }
}
