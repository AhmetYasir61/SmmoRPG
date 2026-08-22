package com.smmorpg.combat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.EquipmentSlot;

/** What the blade actually met. Drives which of the 60 impact sounds plays. */
public enum ImpactMaterial {
    FLESH("flesh"),
    BONE("bone"),
    LEATHER("leather"),
    CHAIN("chain"),
    PLATE("plate"),
    SHIELD("shield"),
    STONE("stone"),
    UNDEAD("undead");

    private final String key;
    ImpactMaterial(String key) { this.key = key; }
    public String key() { return key; }

    /** Armour on the struck part wins over the creature's own material. */
    public static ImpactMaterial resolve(LivingEntity target, HitLocation loc, boolean blocked) {
        if (blocked) return SHIELD;

        EquipmentSlot slot = slotFor(loc);
        if (slot != null) {
            ItemStack armour = target.getItemBySlot(slot);
            ImpactMaterial fromArmour = fromArmour(armour);
            if (fromArmour != null) return fromArmour;
        }

        EntityType<?> type = target.getType();
        if (type == EntityType.SKELETON || type == EntityType.STRAY || type == EntityType.WITHER_SKELETON) return BONE;
        if (type == EntityType.ZOMBIE || type == EntityType.HUSK || type == EntityType.DROWNED
                || type == EntityType.ZOMBIE_VILLAGER || type == EntityType.ZOMBIFIED_PIGLIN) return UNDEAD;
        if (type == EntityType.IRON_GOLEM) return PLATE;
        if (type == EntityType.STRAY || type == EntityType.SILVERFISH) return STONE;

        // A clean strike on head or limb hits bone almost immediately.
        return (loc == HitLocation.HEAD || loc.isLimb()) ? BONE : FLESH;
    }

    private static ImpactMaterial fromArmour(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ArmorItem armor)) return null;
        Item item = armor;
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath();
        if (id.startsWith("leather")) return LEATHER;
        if (id.startsWith("chainmail")) return CHAIN;
        if (id.startsWith("iron") || id.startsWith("golden") || id.startsWith("diamond")
                || id.startsWith("netherite")) return PLATE;
        return LEATHER;
    }

    private static EquipmentSlot slotFor(HitLocation loc) {
        return switch (loc) {
            case HEAD, NECK -> EquipmentSlot.HEAD;
            case TORSO, LEFT_ARM, RIGHT_ARM -> EquipmentSlot.CHEST;
            case LEFT_LEG, RIGHT_LEG -> EquipmentSlot.LEGS;
            default -> null;
        };
    }
}
