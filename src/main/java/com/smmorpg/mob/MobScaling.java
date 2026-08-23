package com.smmorpg.mob;

import com.smmorpg.core.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Turns an archetype and a level into an actual opponent.
 *
 * <p>Applied once at spawn and again whenever a mob evolves, because an evolution that does
 * not change what you are fighting is only a rename.
 */
public final class MobScaling {

    /** Effects are refreshed rather than permanent, so a dispel is still worth something. */
    private static final int AURA_DURATION = 20 * 60 * 20;

    private MobScaling() {}

    public static MobData dataOf(LivingEntity entity) {
        return entity.getData(ModAttachments.MOB.get());
    }

    public static void setData(LivingEntity entity, MobData data) {
        entity.setData(ModAttachments.MOB.get(), data);
    }

    /** Stamps a fresh opponent: stats, effects, gear and a name that says what it is. */
    public static void apply(LivingEntity entity, MobArchetype archetype, int level) {
        MobData data = new MobData(archetype.key(), archetype.tier().key(), level, 0, false);
        setData(entity, data);
        applyStats(entity, archetype, data);
        applyAuras(entity, archetype);
        applyEquipment(entity, archetype);
        applyName(entity, archetype, data);

        if (entity instanceof Mob mob) mob.setPersistenceRequired();
        entity.setHealth(entity.getMaxHealth());
    }

    /** Re-applies everything after a level or tier change, keeping current health fraction. */
    public static void reapply(LivingEntity entity) {
        MobData data = dataOf(entity);
        MobArchetype archetype = data.archetypeOf();
        if (archetype == null) return;

        float fraction = entity.getMaxHealth() <= 0.0F
                ? 1.0F : entity.getHealth() / entity.getMaxHealth();

        applyStats(entity, archetype, data);
        applyAuras(entity, archetype);
        applyName(entity, archetype, data);
        // Evolving heals proportionally rather than fully: growing stronger mid-fight
        // should not undo the fight you have already won.
        entity.setHealth(Math.max(1.0F, entity.getMaxHealth() * fraction));
    }

    private static void applyStats(LivingEntity entity, MobArchetype archetype, MobData data) {
        float scale = data.statMultiplier();

        set(entity, Attributes.MAX_HEALTH, archetype.healthScale() * scale);
        set(entity, Attributes.ATTACK_DAMAGE, archetype.damageScale() * scale);
        set(entity, Attributes.ARMOR, 1.0F + scale * 0.35F, true);
        set(entity, Attributes.KNOCKBACK_RESISTANCE, Math.min(1.0F, scale * 0.12F), true);
        set(entity, Attributes.FOLLOW_RANGE, 3.0F, true);

        AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            // Speed is capped hard. A mob you cannot disengage from is not difficult,
            // it is unplayable.
            double scaled = speed.getBaseValue() * (1.0D + Math.min(0.6D, scale * 0.06D));
            speed.setBaseValue(scaled);
        }
    }

    private static void set(LivingEntity entity, Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                            float multiplier) {
        set(entity, attribute, multiplier, false);
    }

    private static void set(LivingEntity entity, Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                            float value, boolean absolute) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) return;
        instance.setBaseValue(absolute ? value : instance.getBaseValue() * value);
    }

    private static void applyAuras(LivingEntity entity, MobArchetype archetype) {
        for (Holder<MobEffect> effect : archetype.auras()) {
            entity.addEffect(new MobEffectInstance(effect, AURA_DURATION, 0, false, false));
        }
    }

    /** Gear is most of what makes a husk read as a soldier rather than as a husk. */
    private static void applyEquipment(LivingEntity entity, MobArchetype archetype) {
        switch (archetype.equipment()) {
            case NONE -> { }
            case LIGHT -> {
                arm(entity, Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE,
                        Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS, Items.STONE_SWORD);
            }
            case SOLDIER -> {
                arm(entity, Items.CHAINMAIL_HELMET, Items.IRON_CHESTPLATE,
                        Items.CHAINMAIL_LEGGINGS, Items.IRON_BOOTS, Items.IRON_SWORD);
                entity.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            }
            case KNIGHT -> {
                arm(entity, Items.IRON_HELMET, Items.DIAMOND_CHESTPLATE,
                        Items.IRON_LEGGINGS, Items.DIAMOND_BOOTS, Items.DIAMOND_SWORD);
                entity.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            }
            case WARLORD -> {
                arm(entity, Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE,
                        Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS, Items.NETHERITE_AXE);
            }
        }
        if (entity instanceof Mob mob) {
            // Gear stays on the corpse's owner, not on the floor; a farm of free netherite
            // is not what a training arena is for.
            for (EquipmentSlot slot : EquipmentSlot.values()) mob.setDropChance(slot, 0.0F);
        }
    }

    private static void arm(LivingEntity entity, net.minecraft.world.item.Item head,
                            net.minecraft.world.item.Item chest, net.minecraft.world.item.Item legs,
                            net.minecraft.world.item.Item feet, net.minecraft.world.item.Item weapon) {
        entity.setItemSlot(EquipmentSlot.HEAD, new ItemStack(head));
        entity.setItemSlot(EquipmentSlot.CHEST, new ItemStack(chest));
        entity.setItemSlot(EquipmentSlot.LEGS, new ItemStack(legs));
        entity.setItemSlot(EquipmentSlot.FEET, new ItemStack(feet));
        entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(weapon));
    }

    /** "Lv 24 Revenant Lord", coloured by tier, with a Lord marked out in bold. */
    public static void applyName(LivingEntity entity, MobArchetype archetype, MobData data) {
        Component name = Component.translatable(archetype.translationKey());
        if (data.lord()) {
            name = Component.translatable("mob.smmorpg.lord_prefix", name);
        }

        Component full = Component.literal("Lv " + data.level() + " ")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(name.copy().withStyle(data.mobTier().color()));

        entity.setCustomName(data.lord() ? full.copy().withStyle(ChatFormatting.BOLD) : full);
        entity.setCustomNameVisible(true);
    }
}
