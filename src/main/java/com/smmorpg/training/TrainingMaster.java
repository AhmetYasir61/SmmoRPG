package com.smmorpg.training;

import com.smmorpg.SmmoRPG;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * The figure waiting in the camp between waves.
 *
 * <p>It exists so that going deeper is a decision with a moment attached to it. An arena
 * that rolled straight into the next wave would never let anyone stop at the level they
 * had actually mastered; here the fight resumes when someone reaches out and starts it,
 * and not before.
 */
public final class TrainingMaster {

    /** Marks the entity as a master and records whose camp it belongs to. */
    private static final String TAG = "smmorpg:training_master";

    private TrainingMaster() {}

    public static Mob spawn(ServerLevel level, Vec3 centre, java.util.UUID owner) {
        Mob master = EntityType.VILLAGER.create(level);
        if (master == null) return null;

        master.moveTo(centre.x, centre.y + 1.0D, centre.z, 0.0F, 0.0F);
        master.setNoAi(true);
        master.setInvulnerable(true);
        master.setPersistenceRequired();
        master.setSilent(true);
        master.setCustomName(Component.translatable("training.smmorpg.master")
                .withStyle(ChatFormatting.GOLD));
        master.setCustomNameVisible(true);
        master.getPersistentData().putString(TAG, owner.toString());

        if (!level.addFreshEntity(master)) {
            master.discard();
            return null;
        }
        return master;
    }

    public static boolean isMaster(Entity entity, java.util.UUID owner) {
        return entity.getPersistentData().getString(TAG).equals(owner.toString());
    }

    /**
     * Clicking the master starts the next wave.
     *
     * <p>The interaction is always cancelled, even when it does nothing: a villager whose
     * trade screen opened here would be a different, confusing thing standing in the same
     * place.
     */
    @EventBusSubscriber(modid = SmmoRPG.MOD_ID)
    public static final class Interaction {

        @SubscribeEvent
        public static void onInteract(PlayerInteractEvent.EntityInteract event) {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            if (event.getTarget().getPersistentData().getString(TAG).isEmpty()) return;

            event.setCanceled(true);

            TrainingSession session = TrainingManager.of(player);
            if (session == null || !isMaster(event.getTarget(), session.owner())) return;

            session.advance(player);
        }
    }
}
