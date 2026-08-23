package com.smmorpg.account;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * One stack as the vault stores it.
 *
 * <p>Damage is stored explicitly rather than left to the stack's components, because the
 * whole point of a vault that survives a server restart is that a sword you put in at
 * half durability comes back out at half durability. An item that healed itself in storage
 * would make repairs meaningless.
 */
public record VaultItem(String itemId, int count, int damage, String gearJson) {

    public static final Codec<VaultItem> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("item").forGetter(VaultItem::itemId),
            Codec.INT.fieldOf("count").forGetter(VaultItem::count),
            Codec.INT.optionalFieldOf("damage", 0).forGetter(VaultItem::damage),
            Codec.STRING.optionalFieldOf("gear", "").forGetter(VaultItem::gearJson)
    ).apply(i, VaultItem::new));

    public static VaultItem of(ItemStack stack) {
        String gear = "";
        var data = stack.get(com.smmorpg.core.ModDataComponents.GEAR.get());
        if (data != null) {
            gear = com.smmorpg.loot.GearData.CODEC
                    .encodeStart(com.mojang.serialization.JsonOps.INSTANCE, data)
                    .result().map(Object::toString).orElse("");
        }
        return new VaultItem(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                stack.getCount(), stack.getDamageValue(), gear);
    }

    public ItemStack toStack() {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
        if (item == null) return ItemStack.EMPTY;

        ItemStack stack = new ItemStack(item, count);
        stack.setDamageValue(damage);

        if (!gearJson.isEmpty()) {
            try {
                var json = com.google.gson.JsonParser.parseString(gearJson);
                com.smmorpg.loot.GearData.CODEC
                        .parse(com.mojang.serialization.JsonOps.INSTANCE, json)
                        .result()
                        .ifPresent(g -> stack.set(com.smmorpg.core.ModDataComponents.GEAR.get(), g));
            } catch (Exception ignored) {
                // A vault entry from a newer build can carry gear this one cannot read.
                // Losing the affixes is bad; losing the item would be worse.
            }
        }
        return stack;
    }

    /** True when the two entries can share a slot: same item, same wear, same roll. */
    public boolean stacksWith(VaultItem other) {
        return itemId.equals(other.itemId)
                && damage == other.damage
                && gearJson.equals(other.gearJson);
    }

    public VaultItem withCount(int newCount) {
        return new VaultItem(itemId, newCount, damage, gearJson);
    }
}
