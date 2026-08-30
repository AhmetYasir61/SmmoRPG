package com.smmorpg.client.menu;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.smmorpg.SmmoRPG;
import com.smmorpg.account.VaultItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * What you were carrying, so the front page can show it before you are anywhere.
 *
 * <p>The main menu runs before there is a world, a server or a connection, so a live
 * inventory is not something it can have. What it can have is the last one this client
 * saw. That is written here while you play and read back on the title screen, armour and
 * all.
 *
 * <p>Like the profile cache, this file is the client's own note to itself and nothing
 * trusts it. Editing it changes what your own title screen draws and not one thing more —
 * the server has never read it and never will.
 */
public record InventorySnapshot(List<VaultItem> main,
                                List<VaultItem> armour,
                                List<VaultItem> offhand) {

    public static final InventorySnapshot EMPTY =
            new InventorySnapshot(List.of(), List.of(), List.of());

    public static final Codec<InventorySnapshot> CODEC = RecordCodecBuilder.create(i -> i.group(
            VaultItem.CODEC.listOf().fieldOf("main").forGetter(InventorySnapshot::main),
            VaultItem.CODEC.listOf().fieldOf("armour").forGetter(InventorySnapshot::armour),
            VaultItem.CODEC.listOf().fieldOf("offhand").forGetter(InventorySnapshot::offhand)
    ).apply(i, InventorySnapshot::new));

    /** Armour, helmet first, so the column reads top to bottom the way it is worn. */
    private static final EquipmentSlot[] WORN = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private static InventorySnapshot cached;
    private static int writeCooldown;

    private static Path file() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve("smmorpg").resolve("inventory-cache.json");
    }

    public static InventorySnapshot get() {
        if (cached != null) return cached;

        try {
            Path path = file();
            if (Files.exists(path)) {
                cached = CODEC.parse(JsonOps.INSTANCE,
                                JsonParser.parseString(Files.readString(path)))
                        .result().orElse(EMPTY);
            } else {
                cached = EMPTY;
            }
        } catch (Exception e) {
            // A cache that will not parse is a cache worth throwing away, not a crash.
            cached = EMPTY;
        }
        return cached;
    }

    /**
     * Called every client tick while in a world. Writes at most once every few seconds,
     * because this is a convenience for the title screen, not a save file.
     */
    public static void capture(LocalPlayer player) {
        if (--writeCooldown > 0) return;
        writeCooldown = 20 * 5;

        List<VaultItem> main = new ArrayList<>();
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty()) main.add(VaultItem.of(stack));
        }

        List<VaultItem> armour = new ArrayList<>();
        for (EquipmentSlot slot : WORN) {
            ItemStack stack = player.getItemBySlot(slot);
            armour.add(stack.isEmpty() ? null : VaultItem.of(stack));
        }
        // Nulls would break the codec; an empty slot is written as air instead so the
        // column keeps its shape and a missing helmet stays a missing helmet.
        armour.replaceAll(v -> v == null ? new VaultItem("minecraft:air", 0, 0, "") : v);

        ItemStack off = player.getItemBySlot(EquipmentSlot.OFFHAND);
        List<VaultItem> offhand = off.isEmpty() ? List.of() : List.of(VaultItem.of(off));

        save(new InventorySnapshot(List.copyOf(main), List.copyOf(armour), offhand));
    }

    private static void save(InventorySnapshot snapshot) {
        cached = snapshot;
        try {
            Path path = file();
            Files.createDirectories(path.getParent());
            Files.writeString(path, CODEC.encodeStart(JsonOps.INSTANCE, snapshot)
                    .result().map(Object::toString).orElse("{}"));
        } catch (Exception e) {
            SmmoRPG.LOGGER.debug("Could not write the inventory cache", e);
        }
    }

    public List<ItemStack> mainStacks() { return stacks(main); }
    public List<ItemStack> armourStacks() { return stacks(armour); }
    public List<ItemStack> offhandStacks() { return stacks(offhand); }

    private static List<ItemStack> stacks(List<VaultItem> items) {
        List<ItemStack> out = new ArrayList<>(items.size());
        for (VaultItem item : items) out.add(item.toStack());
        return out;
    }
}
