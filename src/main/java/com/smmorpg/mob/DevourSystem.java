package com.smmorpg.mob;

import com.smmorpg.SmmoRPG;
import com.smmorpg.config.CombatConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * Gluttony.
 *
 * <p>A monster that kills another monster eats it, and what it eats it becomes. Devouring
 * its own kind counts double — a thing that turns on its own is the one that gets somewhere.
 * Enough of it and the mob levels; enough levels and it climbs a tier; past that it becomes
 * a <em>Lord</em>, keeps the name, and is a genuinely different fight from the thing that
 * walked in.
 *
 * <p>This runs everywhere, not only in the arena. Leave a field of monsters alone long
 * enough and something in it will have eaten the rest by the time you come back — which is
 * the point. The world should not wait politely at the difficulty you left it.
 */
@EventBusSubscriber(modid = SmmoRPG.MOD_ID)
public final class DevourSystem {

    /** Devour points needed for one level. */
    private static final int POINTS_PER_LEVEL = 3;
    /** Levels needed before a tier climb is even considered. */
    private static final int LEVELS_PER_TIER = 6;
    /** Levels at which a mob stops being a monster and becomes a Lord. */
    private static final int LORD_LEVEL = 24;

    private DevourSystem() {}

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;
        if (!CombatConfig.CFG.enableDevouring.get()) return;

        // A player kill is a player kill. Only monsters eat each other.
        if (!(event.getSource().getEntity() instanceof LivingEntity killer)) return;
        if (killer instanceof Player || victim instanceof Player) return;
        if (killer == victim) return;

        devour(killer, victim);
    }

    /** Feeds {@code victim} to {@code killer} and evolves it if that was enough. */
    public static void devour(LivingEntity killer, LivingEntity victim) {
        MobData data = MobScaling.dataOf(killer);
        if (!data.initialised()) {
            // Something unmanaged made a kill. Give it an identity so it can start climbing
            // like everything else rather than staying outside the system forever.
            data = adopt(killer);
            if (data == null) return;
        }

        boolean sameKind = killer.getType() == victim.getType();
        int gain = sameKind ? 2 : 1;

        MobData fed = data.withDevoured(data.devoured() + gain);
        int newLevel = data.level() + fed.devoured() / POINTS_PER_LEVEL;

        if (newLevel > data.level()) {
            fed = fed.withDevoured(fed.devoured() % POINTS_PER_LEVEL);
            promote(killer, fed, newLevel, victim);
        } else {
            MobScaling.setData(killer, fed);
        }
    }

    private static void promote(LivingEntity killer, MobData fed, int newLevel,
                                LivingEntity victim) {
        MobTier tier = fed.mobTier();
        boolean climbed = false;

        if (newLevel / LEVELS_PER_TIER > fed.level() / LEVELS_PER_TIER) {
            MobTier next = tier.next();
            climbed = next != tier;
            tier = next;
        }

        boolean becameLord = !fed.lord() && newLevel >= LORD_LEVEL;

        MobData evolved = fed.evolved(newLevel, tier, becameLord);
        MobScaling.setData(killer, evolved);
        MobScaling.reapply(killer);

        if (killer.level() instanceof ServerLevel level) {
            announce(level, killer, evolved, climbed, becameLord);
        }
    }

    /**
     * Gives an unmanaged mob a place in the roster so it can level like the rest.
     * Returns null when nothing in the table matches what it is.
     */
    private static MobData adopt(LivingEntity entity) {
        for (MobTier tier : MobTier.values()) {
            for (MobArchetype archetype : MobRoster.ofTier(tier)) {
                if (archetype.type() == entity.getType()) {
                    MobData data = new MobData(archetype.key(), MobTier.MORTAL.key(), 1, 0, false);
                    MobScaling.setData(entity, data);
                    return data;
                }
            }
        }
        return null;
    }

    /** A mob crossing a threshold should be visible from across the arena. */
    private static void announce(ServerLevel level, LivingEntity killer, MobData data,
                                 boolean climbedTier, boolean becameLord) {
        MobArchetype archetype = data.archetypeOf();
        if (archetype != null) MobScaling.applyName(killer, archetype, data);

        if (!climbedTier && !becameLord) return;

        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                killer.getX(), killer.getY() + killer.getBbHeight() * 0.5D, killer.getZ(),
                becameLord ? 90 : 35, 0.6D, 0.9D, 0.6D, 0.06D);

        level.playSound(null, killer.blockPosition(),
                becameLord ? SoundEvents.WITHER_SPAWN : SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.HOSTILE, becameLord ? 1.4F : 0.8F, becameLord ? 0.7F : 1.2F);

        if (becameLord) {
            // Only a Lord is worth interrupting anyone for.
            Component name = killer.getCustomName() == null
                    ? Component.translatable(archetype == null ? "" : archetype.translationKey())
                    : killer.getCustomName();
            for (Player player : level.players()) {
                if (player.distanceToSqr(killer) > 64.0D * 64.0D) continue;
                player.sendSystemMessage(Component.translatable("msg.smmorpg.lord_born", name)
                        .withStyle(ChatFormatting.DARK_RED));
            }
        }
    }

    /** True when this entity is something the mod is managing. */
    public static boolean isManaged(LivingEntity entity) {
        return MobScaling.dataOf(entity).initialised();
    }

    /** Convenience for other systems that want to know how dangerous a mob has become. */
    public static int levelOf(LivingEntity entity) {
        return MobScaling.dataOf(entity).level();
    }

    public static boolean isLord(LivingEntity entity) {
        return MobScaling.dataOf(entity).lord();
    }

    static boolean isOverworldDay(Level level) {
        return level.dimensionType().hasSkyLight() && level.isDay();
    }
}
