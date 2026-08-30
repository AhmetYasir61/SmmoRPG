package com.smmorpg.training;

import com.smmorpg.loot.GearData;
import com.smmorpg.loot.LootRoller;
import com.smmorpg.vault.DurabilityRules;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * What the smith and the craftsman actually do.
 *
 * <p>Both work on the item in your hand and both are paid in emeralds, which are the only
 * thing the arena's dead leave behind. That keeps the camp honest: every repair and every
 * upgrade is bought with fights you have already won, so a bad wave costs you the next
 * repair rather than nothing at all.
 */
public final class CampServices {

    /** Emeralds per repair, scaled by how badly the thing is beaten up. */
    private static final int MAX_REPAIR_COST = 12;

    private CampServices() {}

    // --- the smith ---

    public static void repair(ServerPlayer player) {
        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);

        if (held.isEmpty() || !held.isDamageableItem()) {
            say(player, "camp.smmorpg.smith.nothing", ChatFormatting.GRAY);
            return;
        }
        if (held.getDamageValue() <= 0) {
            say(player, "camp.smmorpg.smith.already", ChatFormatting.GRAY);
            return;
        }

        int cost = repairCost(held);
        if (!take(player, cost)) {
            sayCost(player, "camp.smmorpg.need_emeralds", cost);
            return;
        }

        held.setDamageValue(0);
        player.sendSystemMessage(Component.translatable("camp.smmorpg.smith.done",
                held.getHoverName(), cost).withStyle(ChatFormatting.AQUA));
    }

    private static int repairCost(ItemStack stack) {
        float wear = DurabilityRules.wear(stack);
        return Math.max(1, Math.round(wear * MAX_REPAIR_COST));
    }

    // --- the craftsman ---

    /**
     * Adds one more affix to the held gear, up to what its rarity has room for.
     *
     * <p>It cannot make a common blade legendary, only fill the slots that blade already
     * has. An upgrade path that ignored rarity would make every drop interchangeable, and
     * the whole point of a rarity is that some things are worth keeping.
     */
    public static void improve(ServerPlayer player) {
        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);

        if (held.isEmpty()) {
            say(player, "camp.smmorpg.craftsman.nothing", ChatFormatting.GRAY);
            return;
        }

        GearData data = LootRoller.of(held);
        if (data == null) data = GearData.EMPTY;

        int slots = data.tier().affixSlots();
        if (data.affixes().size() >= slots) {
            say(player, "camp.smmorpg.craftsman.full", ChatFormatting.GRAY);
            return;
        }

        int cost = improveCost(data.affixes().size());
        if (!take(player, cost)) {
            sayCost(player, "camp.smmorpg.need_emeralds", cost);
            return;
        }

        // Roll a fresh piece at the same rarity and borrow one affix it does not have.
        GearData rolled = LootRoller.rollGear(player.getRandom(), data.itemLevel(), 0.0F);
        var affixes = new java.util.ArrayList<>(data.affixes());
        for (var candidate : rolled.affixes()) {
            boolean already = affixes.stream().anyMatch(a -> a.type() == candidate.type());
            if (!already) {
                affixes.add(candidate);
                break;
            }
        }

        GearData improved = new GearData(data.rarity(), data.itemLevel() + 1,
                java.util.List.copyOf(affixes), data.name());
        LootRoller.apply(held, improved);

        player.sendSystemMessage(Component.translatable("camp.smmorpg.craftsman.done",
                held.getHoverName(), cost).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private static int improveCost(int existingAffixes) {
        // Each affix already on the item makes the next one dearer, so a finished weapon
        // is something you worked towards rather than something you bought in one go.
        return 8 + existingAffixes * 10;
    }

    // --- emeralds ---

    public static int emeralds(ServerPlayer player) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(Items.EMERALD)) total += stack.getCount();
        }
        return total;
    }

    /** Takes the price, or nothing at all if the player cannot pay it in full. */
    public static boolean take(ServerPlayer player, int price) {
        if (price <= 0) return true;
        if (emeralds(player) < price) return false;

        int left = price;
        for (ItemStack stack : player.getInventory().items) {
            if (left <= 0) break;
            if (!stack.is(Items.EMERALD)) continue;

            int taken = Math.min(left, stack.getCount());
            stack.shrink(taken);
            left -= taken;
        }
        player.containerMenu.broadcastChanges();
        return true;
    }

    private static void say(ServerPlayer player, String key, ChatFormatting colour) {
        player.sendSystemMessage(Component.translatable(key).withStyle(colour));
    }

    private static void sayCost(ServerPlayer player, String key, int cost) {
        player.sendSystemMessage(Component.translatable(key, cost, emeralds(player))
                .withStyle(ChatFormatting.RED));
    }
}
