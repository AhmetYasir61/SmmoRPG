package com.smmorpg.kit;

import com.smmorpg.capability.PlayerProgress;
import com.smmorpg.classes.PlayerClass;
import com.smmorpg.core.ModAttachments;
import com.smmorpg.core.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * What you start a fight with.
 *
 * <p>A training arena you walk into bare-handed is not a test of anything: the point of
 * setting the difficulty is to find out where your kit stops being enough, and you cannot
 * find that out without a kit. So the arena hands you your class's weapon and enough
 * armour to survive the first exchange, every time you start a session.
 *
 * <p>The kit is deliberately plain — no affixes, no rarity roll. It is the baseline the
 * loot you actually earn is measured against, and a starter weapon that rolled well would
 * make the first hour of drops feel like a downgrade.
 */
public final class StarterKit {

    private StarterKit() {}

    /**
     * Gives whatever the player is missing. Items they already carry are not duplicated,
     * so starting a second session does not bury you in bandages.
     */
    public static void grant(ServerPlayer player) {
        PlayerProgress progress = player.getData(ModAttachments.PROGRESS.get());

        for (ItemStack stack : contents(progress.playerClass())) {
            if (!player.getInventory().contains(s -> ItemStack.isSameItem(s, stack))) {
                give(player, stack);
            }
        }
        equipArmour(player);
    }

    /** The kit itself, class by class. */
    public static List<ItemStack> contents(PlayerClass klass) {
        List<ItemStack> kit = new ArrayList<>();

        switch (klass) {
            case SHINOBI -> kit.add(new ItemStack(ModItems.KATANA.get()));
            case RONIN -> kit.add(new ItemStack(ModItems.ODACHI.get()));
            case ASSASSIN -> {
                kit.add(new ItemStack(ModItems.DAGGER.get()));
                kit.add(new ItemStack(ModItems.TANTO.get()));
            }
            case LANCER -> kit.add(new ItemStack(ModItems.SPEAR.get()));
            case MONK -> kit.add(new ItemStack(ModItems.JIAN.get()));
            case ONMYOJI -> kit.add(new ItemStack(ModItems.DAO.get()));
            case KYUDOKA -> {
                kit.add(new ItemStack(ModItems.YUMI.get()));
                kit.add(new ItemStack(Items.ARROW, 64));
            }
            case BULWARK -> {
                kit.add(new ItemStack(ModItems.KANABO.get()));
                kit.add(new ItemStack(Items.SHIELD));
            }
        }

        // The wound system is only playable if you can treat wounds. Sending someone into a
        // fight that makes them bleed without the means to stop it is not difficulty.
        kit.add(new ItemStack(ModItems.BANDAGE.get(), 8));
        kit.add(new ItemStack(ModItems.CAUTERY_IRON.get()));
        kit.add(new ItemStack(ModItems.BLOOD_VIAL.get(), 2));
        kit.add(new ItemStack(Items.COOKED_BEEF, 16));

        return kit;
    }

    /** Iron, and only where the slot is empty. Nobody's own armour gets replaced. */
    private static void equipArmour(ServerPlayer player) {
        put(player, EquipmentSlot.HEAD, Items.IRON_HELMET);
        put(player, EquipmentSlot.CHEST, Items.IRON_CHESTPLATE);
        put(player, EquipmentSlot.LEGS, Items.IRON_LEGGINGS);
        put(player, EquipmentSlot.FEET, Items.IRON_BOOTS);
    }

    private static void put(ServerPlayer player, EquipmentSlot slot, net.minecraft.world.item.Item item) {
        if (player.getItemBySlot(slot).isEmpty()) {
            player.setItemSlot(slot, new ItemStack(item));
        }
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        ItemStack copy = stack.copy();
        if (!player.getInventory().add(copy) && !copy.isEmpty()) {
            player.drop(copy, false);
        }
    }
}
