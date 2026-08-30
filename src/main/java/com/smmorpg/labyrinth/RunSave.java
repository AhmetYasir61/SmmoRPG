package com.smmorpg.labyrinth;

import com.smmorpg.account.VaultItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * A save: where you were, what you were carrying, and how many lives it bought you.
 *
 * <p>The inventory is part of the save on purpose. Without it a checkpoint is only a
 * teleport, and nothing you find between two of them is ever really at risk — you could
 * grab a legendary, log out, and keep it whatever happened next. Storing what you had
 * makes the walk between saves the thing you are actually gambling.
 *
 * <p>The vault is the way out of that gamble. Anything you put in it at a save is yours
 * for good, because the vault is not part of the run.
 */
public record RunSave(long cell, int lives, List<VaultItem> inventory, List<VaultItem> armour) {

    /** How many deaths a save is worth before it stops being the one you fall back to. */
    public static final int LIVES = 5;

    private static final EquipmentSlot[] WORN = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
            EquipmentSlot.FEET, EquipmentSlot.OFFHAND
    };

    /** Takes the picture: everything the player is carrying and wearing, right now. */
    public static RunSave of(ServerPlayer player, long cell) {
        List<VaultItem> items = new ArrayList<>();
        for (ItemStack stack : player.getInventory().items) {
            items.add(stack.isEmpty() ? empty() : VaultItem.of(stack));
        }

        List<VaultItem> worn = new ArrayList<>();
        for (EquipmentSlot slot : WORN) {
            ItemStack stack = player.getItemBySlot(slot);
            worn.add(stack.isEmpty() ? empty() : VaultItem.of(stack));
        }

        return new RunSave(cell, LIVES, List.copyOf(items), List.copyOf(worn));
    }

    /**
     * Puts the picture back.
     *
     * <p>Slot for slot, including the empty ones, so restoring is a restore rather than a
     * merge — anything picked up since the save is simply not there any more.
     */
    public void restore(ServerPlayer player) {
        var inv = player.getInventory();

        for (int i = 0; i < inv.items.size(); i++) {
            inv.items.set(i, i < inventory.size() ? inventory.get(i).toStack() : ItemStack.EMPTY);
        }
        for (int i = 0; i < WORN.length; i++) {
            player.setItemSlot(WORN[i], i < armour.size() ? armour.get(i).toStack() : ItemStack.EMPTY);
        }

        player.containerMenu.broadcastChanges();
        player.inventoryMenu.broadcastChanges();
    }

    public RunSave withLives(int remaining) {
        return new RunSave(cell, remaining, inventory, armour);
    }

    private static VaultItem empty() { return new VaultItem("minecraft:air", 0, 0, ""); }

    // --- storage ---

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("cell", cell);
        tag.putInt("lives", lives);
        tag.put("inventory", writeList(inventory));
        tag.put("armour", writeList(armour));
        return tag;
    }

    public static RunSave fromTag(CompoundTag tag) {
        return new RunSave(tag.getLong("cell"), tag.getInt("lives"),
                readList(tag.getList("inventory", Tag.TAG_COMPOUND)),
                readList(tag.getList("armour", Tag.TAG_COMPOUND)));
    }

    private static ListTag writeList(List<VaultItem> items) {
        ListTag list = new ListTag();
        for (VaultItem item : items) {
            CompoundTag entry = new CompoundTag();
            entry.putString("item", item.itemId());
            entry.putInt("count", item.count());
            entry.putInt("damage", item.damage());
            entry.putString("gear", item.gearJson());
            list.add(entry);
        }
        return list;
    }

    private static List<VaultItem> readList(ListTag list) {
        List<VaultItem> items = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            items.add(new VaultItem(entry.getString("item"), entry.getInt("count"),
                    entry.getInt("damage"), entry.getString("gear")));
        }
        return List.copyOf(items);
    }
}
