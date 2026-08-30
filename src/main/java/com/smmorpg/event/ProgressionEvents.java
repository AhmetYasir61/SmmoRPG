package com.smmorpg.event;

import com.smmorpg.SmmoRPG;
import com.smmorpg.capability.PlayerProgress;
import com.smmorpg.classes.PlayerClass;
import com.smmorpg.core.ModAttachments;
import com.smmorpg.core.ModAttributes;
import com.smmorpg.core.ModItems;
import com.smmorpg.loot.GearData;
import com.smmorpg.loot.LootRoller;
import com.smmorpg.network.Net;
import com.smmorpg.network.S2CProgressSync;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.ArrayList;
import java.util.List;

/** Levelling, stat application and the level-scaled loot drop. */
@EventBusSubscriber(modid = SmmoRPG.MOD_ID)
public final class ProgressionEvents {

    /**
     * What a kill can drop.
     *
     * <p>Deliberately not a fixed list of this mod's own items. Affixes are stamped onto a
     * data component, so anything that swings — a vanilla sword, a Weapons of Miracles
     * blade, an Epic Fight weapon from any pack — can carry them. The pool is built at
     * runtime from every registered weapon, which means installing another weapon mod
     * widens the loot table without a line of code here.
     */
    private static List<Item> dropPool(RandomSource rng) {
        List<Item> pool = new ArrayList<>(ModItems.ALL.size());
        for (var holder : ModItems.ALL) pool.add(holder.get());

        // Anything the game considers a weapon is fair game to roll affixes onto.
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof SwordItem || item instanceof AxeItem || item instanceof TridentItem
                    || item instanceof BowItem || item instanceof CrossbowItem) {
                pool.add(item);
            }
        }
        return pool;
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) return;

        PlayerProgress progress = killer.getData(ModAttachments.PROGRESS.get());
        int before = progress.level();

        int xp = experienceFor(dead);
        PlayerProgress next = progress.grantExperience(xp);
        killer.setData(ModAttachments.PROGRESS.get(), next);

        if (next.level() > before) {
            applyClassStats(killer, next);
            killer.sendSystemMessage(Component.translatable("msg.smmorpg.level_up", next.level()));
            killer.level().playSound(null, killer.blockPosition(),
                    net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.0F);
        }
        Net.sendTo(killer, new S2CProgressSync(next));

        rollDrop(killer, dead, next);
    }

    /** Loot is never a fixed table entry: it is generated from the kill and the killer. */
    private static void rollDrop(ServerPlayer killer, LivingEntity dead, PlayerProgress progress) {
        var rng = killer.level().random;
        float difficulty = dead.getMaxHealth() / 20.0F;
        float dropChance = Math.min(0.55F, 0.06F + difficulty * 0.04F);
        if (rng.nextFloat() > dropChance) return;

        float luck = (float) killer.getAttributeValue(Attributes.LUCK) + progress.spirit() * 0.02F;
        GearData data = LootRoller.rollGear(rng, progress.level(), luck);
        List<Item> pool = dropPool(rng);
        ItemStack stack = new ItemStack(pool.get(rng.nextInt(pool.size())));
        LootRoller.apply(stack, data);

        dead.spawnAtLocation(stack);

        if (data.holy() || data.cursed()) {
            killer.sendSystemMessage(Component.translatable(
                    data.holy() ? "msg.smmorpg.holy_drop" : "msg.smmorpg.cursed_drop",
                    stack.getHoverName()).withStyle(data.tier().color()));
        }
    }

    private static int experienceFor(LivingEntity dead) {
        int base = Math.round(dead.getMaxHealth() * 1.6F
                + (float) dead.getAttributeValue(Attributes.ATTACK_DAMAGE) * 4.0F);
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(dead.getType()).getPath();
        if (id.contains("boss") || id.equals("wither") || id.equals("ender_dragon")) base *= 12;
        return Math.max(1, base);
    }

    /** Pushes class and stat numbers onto the vanilla attribute system. */
    public static void applyClassStats(Player player, PlayerProgress progress) {
        PlayerClass pc = progress.playerClass();

        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) health.setBaseValue(progress.maxHealth());

        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(0.1D * (1.0D + progress.agility() * 0.004D));

        AttributeInstance atkSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (atkSpeed != null) atkSpeed.setBaseValue(4.0D * pc.attackSpeedMultiplier()
                * (1.0D + progress.agility() * 0.003D));

        AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
        if (armor != null) armor.setBaseValue(Math.max(0.0D, (pc.defenseMultiplier() - 1.0D) * 6.0D));

        AttributeInstance posture = player.getAttribute(ModAttributes.POSTURE.getDelegate());
        if (posture != null) posture.setBaseValue(100.0D + progress.vitality() * 2.0D);

        AttributeInstance bleedRes = player.getAttribute(ModAttributes.BLEED_RESISTANCE.getDelegate());
        if (bleedRes != null) bleedRes.setBaseValue(Math.min(0.8D, progress.vitality() * 0.008D));

        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        PlayerProgress progress = sp.getData(ModAttachments.PROGRESS.get());
        applyClassStats(sp, progress);
        Net.sendTo(sp, new S2CProgressSync(progress));
        Net.sendTo(sp, new com.smmorpg.network.S2CTrainingLevel(
                sp.getData(ModAttachments.TRAINING_LEVEL.get())));
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        PlayerProgress progress = sp.getData(ModAttachments.PROGRESS.get());
        applyClassStats(sp, progress);
        sp.setData(ModAttachments.WOUNDS.get(), com.smmorpg.wound.WoundData.EMPTY);
        Net.sendTo(sp, new S2CProgressSync(progress));
    }

    @SubscribeEvent
    public static void onJoinLevel(EntityJoinLevelEvent event) {
        // Nothing to do yet, but this is where per-entity combat state would be primed
        // for mobs that need a non-default posture pool.
    }
}
