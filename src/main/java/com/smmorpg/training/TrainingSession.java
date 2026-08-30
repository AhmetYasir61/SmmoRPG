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
 * <p>Runs in two phases. During a wave it spawns opponents at the level's difficulty and
 * drives their brains every tick. When the wave is cleared it drops into a rest camp:
 * nothing spawns, the player heals, and a training master stands in the middle of the
 * floor. Nothing continues until they are spoken to — so the fight only ever resumes
 * because the player decided it should, which is the difference between a gauntlet and a
 * treadmill.
 *
 * <p>A session is per player, so two people can be at different levels in the same world
 * without interfering.
 */
public class TrainingSession {

    private final UUID owner;
    private final Vec3 centre;
    private final Map<Mob, CombatBotBrain> bots = new HashMap<>();

    private int level;
    private int respawnTimer = 0;
    private int killsThisWave = 0;
    private int kills = 0;

    /** True between waves: no spawns, and a training master waiting to be spoken to. */
    private boolean resting;
    private Mob master;

    public TrainingSession(UUID owner, int level, Vec3 centre) {
        this.owner = owner;
        this.level = level;
        this.centre = centre;
    }

    public UUID owner() { return owner; }
    public Difficulty difficulty() { return TrainingLevels.difficultyFor(level); }
    public int level() { return level; }
    public int wave() { return level; }
    public int kills() { return kills; }
    public int killsThisWave() { return killsThisWave; }
    public int killsNeeded() { return TrainingLevels.killsFor(level); }
    public boolean resting() { return resting; }
    public Vec3 centre() { return centre; }

    /** Lays the arena down the first time this session ticks in a world. */
    private boolean built;

    public void tick(ServerLevel world, ServerPlayer player) {
        if (!built) {
            built = true;
            TrainingArena.build(world, centre);
        }

        Difficulty difficulty = difficulty();

        // Retire anything that died and count it.
        Iterator<Map.Entry<Mob, CombatBotBrain>> it = bots.entrySet().iterator();
        while (it.hasNext()) {
            Mob mob = it.next().getKey();
            if (!mob.isAlive() || mob.isRemoved()) {
                it.remove();
                kills++;
                killsThisWave++;
            }
        }

        if (resting) {
            tickCamp(world, player);
            return;
        }

        for (Map.Entry<Mob, CombatBotBrain> e : bots.entrySet()) {
            e.getValue().tick(e.getKey(), player);
        }

        if (killsThisWave >= killsNeeded() && bots.isEmpty()) {
            beginRest(world, player);
            return;
        }

        // Stop feeding the wave once enough have been sent; the rest of it is the player
        // finishing what is already on the floor.
        int spawnedOrLeft = killsThisWave + bots.size();
        if (spawnedOrLeft < killsNeeded() && bots.size() < difficulty.simultaneousOpponents()) {
            if (--respawnTimer <= 0) {
                respawnTimer = Math.max(10, 60 - difficulty.band() * 2);
                spawnOne(world, player);
            }
        }
    }

    // --- the camp between waves ---

    private void beginRest(ServerLevel world, ServerPlayer player) {
        resting = true;
        killsThisWave = 0;

        player.setHealth(player.getMaxHealth());
        player.getFoodData().eat(20, 1.0F);
        player.setData(com.smmorpg.core.ModAttachments.WOUNDS.get(),
                com.smmorpg.wound.WoundData.EMPTY);

        master = TrainingMaster.spawn(world, centre, owner);

        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                "training.smmorpg.camp", level + 1,
                TrainingLevels.percentFor(level + 1))
                .withStyle(net.minecraft.ChatFormatting.AQUA));
    }

    private void tickCamp(ServerLevel world, ServerPlayer player) {
        // The master is the only way onward, so it is put back if anything removes it.
        if (master == null || !master.isAlive() || master.isRemoved()) {
            master = TrainingMaster.spawn(world, centre, owner);
        }
    }

    /** Called when the player clicks the master: one level harder, camp struck. */
    public void advance(ServerPlayer player) {
        if (!resting) return;

        resting = false;
        killsThisWave = 0;
        level = Math.min(TrainingLevels.maxLevel(), level + 1);
        player.setData(com.smmorpg.core.ModAttachments.TRAINING_LEVEL.get(), level);
        com.smmorpg.network.Net.sendTo(player, new com.smmorpg.network.S2CTrainingLevel(level));

        if (master != null) {
            master.discard();
            master = null;
        }

        Difficulty next = difficulty();
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                "training.smmorpg.wave", level, next.percent(),
                net.minecraft.network.chat.Component.translatable(next.tierKey()))
                .withStyle(next.divine()
                        ? net.minecraft.ChatFormatting.GOLD
                        : net.minecraft.ChatFormatting.YELLOW));
    }

    /**
     * Spawns one opponent at the edge of the arena.
     *
     * <p>What arrives comes from {@link MobRoster}, chosen by the difficulty band — so
     * raising the percentage does not only inflate numbers, it changes what walks in. Low
     * bands send soldiers and raiders; high ones send things out of a bestiary, and the
     * top of the table is where the dragons are.
     */
    private void spawnOne(ServerLevel world, ServerPlayer player) {
        var rng = world.random;
        double angle = rng.nextDouble() * Math.PI * 2.0D;
        double radius = 8.0D + rng.nextDouble() * 5.0D;
        BlockPos pos = BlockPos.containing(
                centre.x + Math.cos(angle) * radius,
                centre.y + 1.0D,
                centre.z + Math.sin(angle) * radius);

        Difficulty difficulty = difficulty();
        MobArchetype archetype = MobRoster.roll(rng, difficulty.band());

        Entity spawned = archetype.type().create(world);
        if (!(spawned instanceof Mob mob)) {
            if (spawned != null) spawned.discard();
            return;
        }

        mob.moveTo(pos, (float) Math.toDegrees(-angle), 0.0F);
        try {
            mob.finalizeSpawn(world, world.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        } catch (Exception e) {
            // Bosses and other unusual types can object to being finalised outside their
            // own structure. Their stats are set below regardless, so this is survivable.
            SmmoRPG.LOGGER.debug("finalizeSpawn refused for {}", archetype.key());
        }

        int mobLevel = 1 + difficulty.band() * 4 + rng.nextInt(5);
        MobScaling.apply(mob, archetype, mobLevel);
        applyDifficulty(mob, difficulty);

        if (!world.addFreshEntity(mob)) {
            mob.discard();
            return;
        }
        bots.put(mob, new CombatBotBrain(FightingStyle.random(rng), difficulty, rng));
    }

    /** The chosen percentage on top of whatever the archetype and level already gave it. */
    private void applyDifficulty(Mob mob, Difficulty difficulty) {
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

    public void end(ServerLevel world) {
        for (Mob mob : new ArrayList<>(bots.keySet())) mob.discard();
        bots.clear();
        if (master != null) {
            master.discard();
            master = null;
        }
    }

    public List<Mob> opponents() { return new ArrayList<>(bots.keySet()); }
}
