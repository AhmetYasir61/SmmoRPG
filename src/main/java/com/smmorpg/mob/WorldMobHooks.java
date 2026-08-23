package com.smmorpg.mob;

import com.smmorpg.SmmoRPG;
import com.smmorpg.config.CombatConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Brings the levelling system out of the arena and into the world.
 *
 * <p>A naturally spawned monster is given a tier and a level from how far out it spawned, so
 * the map has a difficulty gradient rather than one flat threat everywhere. From then on it
 * is on the same footing as an arena opponent: it can devour, it can evolve, and it can end
 * up a Lord standing somewhere you were not expecting one.
 */
@EventBusSubscriber(modid = SmmoRPG.MOD_ID)
public final class WorldMobHooks {

    /** Blocks from spawn per difficulty band. Far out is genuinely another country. */
    private static final double BLOCKS_PER_BAND = 900.0D;

    private WorldMobHooks() {}

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        if (!CombatConfig.CFG.levelWorldMobs.get()) return;
        if (!(event.getEntity() instanceof Monster monster)) return;

        MobData existing = MobScaling.dataOf(monster);
        if (existing.initialised()) return;      // already ours, or reloaded from disk

        int band = bandAt(monster);
        var rng = monster.getRandom();
        MobArchetype archetype = MobRoster.roll(rng, band);

        // The roster picks the flavour, but a natural spawn keeps the body it was given —
        // replacing a spawning zombie with a dragon is how a quiet night becomes a crash.
        MobArchetype matched = matchType(archetype, monster);
        if (matched == null) return;

        int level = 1 + band * 3 + rng.nextInt(4);
        MobScaling.apply(monster, matched, level);
    }

    /**
     * Finds an archetype in the rolled tier that this entity can actually be. Returns null
     * when nothing fits, which leaves the mob vanilla rather than mismatched.
     */
    private static MobArchetype matchType(MobArchetype rolled, Monster monster) {
        if (rolled.type() == monster.getType()) return rolled;

        for (MobArchetype candidate : MobRoster.ofTier(rolled.tier())) {
            if (candidate.type() == monster.getType()) return candidate;
        }
        for (MobTier tier : MobTier.values()) {
            for (MobArchetype candidate : MobRoster.ofTier(tier)) {
                if (candidate.type() == monster.getType()) return candidate;
            }
        }
        return null;
    }

    private static int bandAt(Mob mob) {
        double distance = Math.sqrt(mob.blockPosition().distSqr(
                mob.level().getSharedSpawnPos()));
        return (int) Math.min(20.0D, distance / BLOCKS_PER_BAND);
    }

    /**
     * Keeps managed monsters out of the sun.
     *
     * <p>The roster is built from types that do not burn, but gear, evolution and other mods
     * can all put a burning one on the field. Putting the fire out every tick is cheap and
     * it means a fight is never decided by the time of day.
     */
    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!CombatConfig.CFG.preventDaylightBurning.get()) return;
        if (!(event.getEntity() instanceof LivingEntity entity)) return;

        Level level = entity.level();
        if (level.isClientSide) return;
        if (!entity.isOnFire()) return;
        if (!MobScaling.dataOf(entity).initialised()) return;

        // Only daylight is undone. Lava and a flaming sword still work exactly as they should.
        if (DevourSystem.isOverworldDay(level) && level.canSeeSky(entity.blockPosition())) {
            entity.clearFire();
        }
    }
}
