package com.smmorpg.training;

import com.smmorpg.SmmoRPG;
import com.smmorpg.labyrinth.Labyrinth;
import com.smmorpg.labyrinth.LabyrinthData;
import com.smmorpg.mob.MobArchetype;
import com.smmorpg.mob.MobRoster;
import com.smmorpg.mob.MobScaling;
import com.smmorpg.npc.CombatBotBrain;
import com.smmorpg.npc.FightingStyle;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One player's run through the labyrinth.
 *
 * <p>The labyrinth itself belongs to the world and outlives every session — this is the
 * part that belongs to the player: what level they are fighting at, how much of the wave
 * is left, and which opponents are currently theirs.
 *
 * <p>Opponents spawn in the cell the player is standing in and the ones around it, never
 * in a safe cell, and never once the wave is cleared. So a safe cell really is safe, and
 * clearing a wave really does end the fighting rather than merely slowing it down.
 */
public class TrainingSession {

    /** How far ahead of the player the labyrinth is written into the world, in cells. */
    private static final int BUILD_RADIUS = 2;

    private final UUID owner;
    private final Map<Mob, CombatBotBrain> bots = new java.util.HashMap<>();
    private final Map<CampNpc.Role, Mob> staff = new EnumMap<>(CampNpc.Role.class);

    private int level;
    private int respawnTimer;
    private int killsThisWave;
    private int kills;

    /** Set when the wave's quota is met: nothing more spawns until the camp says so. */
    private boolean waveCleared;

    /** How much harder this run is for the number of people in it. Recomputed each wave. */
    private float pressure = 1.0F;

    private com.smmorpg.shop.ShopStock stock = com.smmorpg.shop.ShopStock.EMPTY;
    private int rerolls;

    public TrainingSession(UUID owner, int level) {
        this.owner = owner;
        this.level = level;
    }

    public UUID owner() { return owner; }
    public Difficulty difficulty() { return TrainingLevels.difficultyFor(level); }
    public int level() { return level; }
    public int kills() { return kills; }
    public int killsThisWave() { return killsThisWave; }
    /** A wave grows with the party, so sixteen people do not clear it in one sweep. */
    public int killsNeeded() {
        return Math.round(TrainingLevels.killsFor(level) * pressure);
    }
    public boolean waveCleared() { return waveCleared; }
    public float pressure() { return pressure; }

    // --- the tick ---

    public void tick(ServerLevel world, ServerPlayer player) {
        // Recomputed rather than cached at the door: people join and leave a run, and the
        // dungeon should notice within a wave rather than at the next session.
        if (player.tickCount % 40 == 0) {
            pressure = com.smmorpg.party.Party.pressure(
                    com.smmorpg.party.PartyManager.group(player));
        }

        Labyrinth.ensureAround(world, player.position(), BUILD_RADIUS);
        Labyrinth.tickBuild(world);

        ensureCamp(world);

        Iterator<Map.Entry<Mob, CombatBotBrain>> it = bots.entrySet().iterator();
        while (it.hasNext()) {
            Mob mob = it.next().getKey();
            if (!mob.isAlive() || mob.isRemoved()) {
                it.remove();
                kills++;
                killsThisWave++;
            }
        }

        for (Map.Entry<Mob, CombatBotBrain> e : bots.entrySet()) {
            e.getValue().tick(e.getKey(), player);
        }

        if (!waveCleared && killsThisWave >= killsNeeded()) {
            clearWave(player);
            return;
        }
        if (waveCleared) return;

        long here = Labyrinth.cellAt(player.position());
        if (Labyrinth.isSafe(world, here)) return;

        Difficulty difficulty = difficulty();
        int allowed = Math.round(difficulty.simultaneousOpponents() * pressure);
        if (bots.size() >= allowed) return;

        if (--respawnTimer <= 0) {
            respawnTimer = Math.max(10, 60 - difficulty.band() * 2);
            spawnOne(world, player, here);
        }
    }

    private void clearWave(ServerPlayer player) {
        waveCleared = true;
        despawnAll();

        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                        "training.smmorpg.wave_cleared", level + 1,
                        TrainingLevels.percentFor(level + 1))
                .withStyle(net.minecraft.ChatFormatting.AQUA));
    }

    /** Called when the player clicks the master in the camp: one level harder. */
    public void advance(ServerPlayer player) {
        if (!waveCleared) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                            "training.smmorpg.not_yet", killsThisWave, killsNeeded())
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
            return;
        }

        waveCleared = false;
        killsThisWave = 0;
        rerolls = 0;
        stock = com.smmorpg.shop.ShopStock.EMPTY;

        level = Math.min(TrainingLevels.maxLevel(), level + 1);
        player.setData(com.smmorpg.core.ModAttachments.TRAINING_LEVEL.get(), level);
        com.smmorpg.network.Net.sendTo(player, new com.smmorpg.network.S2CTrainingLevel(level));

        Difficulty next = difficulty();
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                        "training.smmorpg.wave", level, next.percent(),
                        net.minecraft.network.chat.Component.translatable(next.tierKey()))
                .withStyle(next.divine()
                        ? net.minecraft.ChatFormatting.GOLD
                        : net.minecraft.ChatFormatting.YELLOW));
    }

    // --- the camp ---

    /** The camp is a place, not an event, so its staff are put back whenever they are gone. */
    private void ensureCamp(ServerLevel world) {
        Vec3 centre = Labyrinth.centreOf(world, Labyrinth.camp());
        for (CampNpc.Role role : CampNpc.Role.values()) {
            Mob npc = staff.get(role);
            if (npc == null || !npc.isAlive() || npc.isRemoved()) {
                Mob replacement = CampNpc.spawn(world, centre, owner, role);
                if (replacement != null) staff.put(role, replacement);
            }
        }
    }

    // --- the merchant ---

    public com.smmorpg.shop.ShopStock shopStock(ServerPlayer player) {
        if (stock.goods().isEmpty() && player.level() instanceof ServerLevel world) {
            stock = CampShop.roll(world.random, level);
        }
        return stock;
    }

    public int rerollCost() { return CampShop.rerollCost(rerolls, level); }

    public com.smmorpg.shop.ShopStock rerollShop(ServerPlayer player) {
        if (player.level() instanceof ServerLevel world) {
            stock = CampShop.roll(world.random, level);
            rerolls++;
        }
        return stock;
    }

    public com.smmorpg.shop.ShopStock markSold(int index) {
        stock = stock.sold(index);
        return stock;
    }

    // --- opponents ---

    /**
     * Spawns one opponent in a cell near the player.
     *
     * <p>What arrives comes from {@link MobRoster}, chosen by the difficulty band, so
     * climbing does not only inflate numbers — it changes what walks around the corner.
     * How deep into the labyrinth the cell is nudges the roll further, which is what makes
     * walking outward feel different from walking in circles.
     */
    private void spawnOne(ServerLevel world, ServerPlayer player, long here) {
        var rng = world.random;

        double angle = rng.nextDouble() * Math.PI * 2.0D;
        double radius = 5.0D + rng.nextDouble() * 6.0D;
        Vec3 target = player.position().add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);

        long cell = Labyrinth.cellAt(target);
        if (Labyrinth.isSafe(world, cell)) return;

        BlockPos pos = BlockPos.containing(target.x, Labyrinth.floorY(world) + 1.0D, target.z);
        if (!world.getBlockState(pos).isAir() || !world.getBlockState(pos.above()).isAir()) return;

        Difficulty difficulty = difficulty();
        int band = difficulty.band() + Math.min(6, Labyrinth.depth(cell) / 8);
        MobArchetype archetype = MobRoster.roll(rng, band);

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

        MobScaling.apply(mob, archetype, 1 + band * 4 + rng.nextInt(5));
        applyDifficulty(mob, difficulty);
        applyPressure(mob);

        if (!world.addFreshEntity(mob)) {
            mob.discard();
            return;
        }
        bots.put(mob, new CombatBotBrain(FightingStyle.random(rng), difficulty, rng));
    }

    /** The level's percentage on top of whatever the archetype already gave it. */
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

    /**
     * What the crowd costs.
     *
     * <p>Only health and damage, not speed or reach. A dungeon that answered a big group by
     * making everything faster would punish the newest member hardest; making everything
     * tougher spreads the weight across people who can share it.
     */
    private void applyPressure(Mob mob) {
        if (pressure <= 1.0F) return;

        var health = mob.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(health.getBaseValue() * pressure);
            mob.setHealth(mob.getMaxHealth());
        }
        var damage = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage != null) {
            // Damage rises more gently than health: a crowd should mean a longer fight
            // before it means a deadlier one.
            damage.setBaseValue(damage.getBaseValue() * (1.0F + (pressure - 1.0F) * 0.5F));
        }
    }

    // --- ending ---

    public void end(ServerLevel world) {
        despawnAll();
        for (Mob npc : staff.values()) {
            if (npc != null) npc.discard();
        }
        staff.clear();
    }

    /**
     * Clears the floor.
     *
     * <p>Stragglers are the reason the old arena was unplayable — something left alive two
     * hundred blocks away with no way back to it. Nothing this session spawned outlives
     * the wave it belonged to.
     */
    private void despawnAll() {
        for (Mob mob : new ArrayList<>(bots.keySet())) mob.discard();
        bots.clear();
    }

    public List<Mob> opponents() { return new ArrayList<>(bots.keySet()); }

    /** Where this player resumes: their last save, or the camp if they have none. */
    public static long resumeCell(ServerLevel world, UUID player) {
        var save = LabyrinthData.get(world).save(player);
        return save == null ? Labyrinth.camp() : save.cell();
    }
}
