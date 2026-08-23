package com.smmorpg.training;

import com.smmorpg.npc.CombatBotBrain;
import com.smmorpg.npc.FightingStyle;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import com.smmorpg.SmmoRPG;
import com.smmorpg.mob.MobArchetype;
import com.smmorpg.mob.MobRoster;
import com.smmorpg.mob.MobScaling;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One player's private training arena.
 *
 * <p>Holds the difficulty, spawns and re-spawns the opponents, and drives their brains
 * every tick. A session is per player, so two people can train at different difficulties
 * in the same world without interfering.
 */
public class TrainingSession {

    private final UUID owner;
    private final Difficulty difficulty;
    private final Vec3 centre;
    private final Map<Mob, CombatBotBrain> bots = new HashMap<>();

    private int respawnTimer = 0;
    private int wave = 0;
    private int kills = 0;

    public TrainingSession(UUID owner, Difficulty difficulty, Vec3 centre) {
        this.owner = owner;
        this.difficulty = difficulty;
        this.centre = centre;
    }

    public UUID owner() { return owner; }
    public Difficulty difficulty() { return difficulty; }
    public int wave() { return wave; }
    public int kills() { return kills; }
    public Vec3 centre() { return centre; }

    /** Lays the arena down the first time this session ticks in a world. */
    private boolean built;

    public void tick(ServerLevel level, ServerPlayer player) {
        if (!built) {
            built = true;
            TrainingArena.build(level, centre);
        }

        // Retire anything that died and count it.
        Iterator<Map.Entry<Mob, CombatBotBrain>> it = bots.entrySet().iterator();
        while (it.hasNext()) {
            Mob mob = it.next().getKey();
            if (!mob.isAlive() || mob.isRemoved()) {
                it.remove();
                kills++;
            }
        }

        for (Map.Entry<Mob, CombatBotBrain> e : bots.entrySet()) {
            e.getValue().tick(e.getKey(), player);
        }

        if (bots.size() < difficulty.simultaneousOpponents()) {
            if (--respawnTimer <= 0) {
                respawnTimer = Math.max(10, 60 - difficulty.band() * 2);
                spawnOne(level, player);
            }
        } else if (bots.isEmpty()) {
            wave++;
        }
    }

    /**
     * Spawns one opponent at the edge of the arena.
     *
     * <p>What arrives comes from {@link MobRoster}, chosen by the difficulty band — so
     * raising the percentage does not only inflate numbers, it changes what walks in. Low
     * bands send soldiers and raiders; high ones send things out of a bestiary, and the
     * top of the table is where the dragons are.
     */
    private void spawnOne(ServerLevel level, ServerPlayer player) {
        var rng = level.random;
        double angle = rng.nextDouble() * Math.PI * 2.0D;
        double radius = 8.0D + rng.nextDouble() * 5.0D;
        BlockPos pos = BlockPos.containing(
                centre.x + Math.cos(angle) * radius,
                centre.y + 1.0D,
                centre.z + Math.sin(angle) * radius);

        MobArchetype archetype = MobRoster.roll(rng, difficulty.band());

        Entity spawned = archetype.type().create(level);
        if (!(spawned instanceof Mob mob)) {
            if (spawned != null) spawned.discard();
            return;
        }

        mob.moveTo(pos, (float) Math.toDegrees(-angle), 0.0F);
        try {
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        } catch (Exception e) {
            // Bosses and other unusual types can object to being finalised outside their
            // own structure. Their stats are set below regardless, so this is survivable.
            SmmoRPG.LOGGER.debug("finalizeSpawn refused for {}", archetype.key());
        }

        int level_ = 1 + difficulty.band() * 4 + rng.nextInt(5);
        MobScaling.apply(mob, archetype, level_);
        applyDifficulty(mob);

        if (!level.addFreshEntity(mob)) {
            mob.discard();
            return;
        }
        bots.put(mob, new CombatBotBrain(FightingStyle.random(rng), difficulty, rng));
    }

    /** The chosen percentage on top of whatever the archetype and level already gave it. */
    private void applyDifficulty(Mob mob) {
        var health = mob.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(health.getBaseValue() * difficulty.healthMultiplier());
            mob.setHealth(mob.getMaxHealth());
        }
        var damage = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage != null) damage.setBaseValue(damage.getBaseValue() * difficulty.damageMultiplier());

        var followRange = mob.getAttribute(Attributes.FOLLOW_RANGE);
        if (followRange != null) followRange.setBaseValue(96.0D);

        var stepHeight = mob.getAttribute(Attributes.STEP_HEIGHT);
        // A high-tier opponent should never be stopped by terrain you can vault.
        if (stepHeight != null) stepHeight.setBaseValue(1.0D + difficulty.acrobatics());
    }

    public void end(ServerLevel level) {
        for (Mob mob : new ArrayList<>(bots.keySet())) mob.discard();
        bots.clear();
    }

    public List<Mob> opponents() { return new ArrayList<>(bots.keySet()); }
}
