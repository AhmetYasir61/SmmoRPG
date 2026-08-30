package com.smmorpg.training;

import com.smmorpg.core.ModItems;
import com.smmorpg.loot.GearData;
import com.smmorpg.loot.LootRoller;
import com.smmorpg.shop.ShopMenu;
import com.smmorpg.shop.ShopStock;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * The merchant's shelf.
 *
 * <p>Eight things, rolled from everything the mod can make — weapons, accessories, the
 * armour you will want by the tenth wave — and never the same eight twice. You can buy
 * what you want and reroll the rest, which is the whole shape of the decision: the reroll
 * is cheap enough to use and dear enough that spending your emeralds on it is a choice
 * rather than a habit.
 */
public final class CampShop {

    /** Everything the merchant can ever have on the shelf. */
    private static final List<Item> POOL = new ArrayList<>();

    private CampShop() {}

    private static synchronized void fillPool() {
        if (!POOL.isEmpty()) return;

        for (var holder : ModItems.ALL) POOL.add(holder.get());

        // Vanilla armour is in the pool because the mod adds none of its own: a shelf that
        // could not sell a chestplate would send everyone back to a crafting table.
        POOL.add(Items.IRON_HELMET);
        POOL.add(Items.IRON_CHESTPLATE);
        POOL.add(Items.IRON_LEGGINGS);
        POOL.add(Items.IRON_BOOTS);
        POOL.add(Items.DIAMOND_HELMET);
        POOL.add(Items.DIAMOND_CHESTPLATE);
        POOL.add(Items.DIAMOND_LEGGINGS);
        POOL.add(Items.DIAMOND_BOOTS);
        POOL.add(Items.SHIELD);
        POOL.add(Items.ARROW);
        POOL.add(Items.GOLDEN_APPLE);
    }

    /** Rolls a whole shelf at the difficulty the player has climbed to. */
    public static ShopStock roll(RandomSource rng, int trainingLevel) {
        fillPool();

        List<ItemStack> goods = new ArrayList<>(ShopStock.SIZE);
        List<Integer> prices = new ArrayList<>(ShopStock.SIZE);

        for (int i = 0; i < ShopStock.SIZE; i++) {
            Item item = POOL.get(rng.nextInt(POOL.size()));
            ItemStack stack = new ItemStack(item);

            if (stack.getMaxStackSize() > 1) {
                stack.setCount(1 + rng.nextInt(Math.min(16, stack.getMaxStackSize())));
            } else {
                // Anything you carry one of is gear, so it arrives rolled rather than plain.
                GearData data = LootRoller.rollGear(rng, Math.max(1, trainingLevel), 0.0F);
                LootRoller.apply(stack, data);
            }

            goods.add(stack);
            prices.add(price(stack, trainingLevel));
        }
        return new ShopStock(goods, prices);
    }

    /**
     * What a thing costs.
     *
     * <p>Driven by what it actually is — its rarity, how many affixes it rolled, how many
     * of it you are getting — rather than by a table, so a lucky shelf is visibly worth
     * saving up for and a dull one is visibly worth rerolling.
     */
    private static int price(ItemStack stack, int trainingLevel) {
        GearData data = LootRoller.of(stack);

        int base = 4 + trainingLevel;
        int rarity = Math.round(data.tier().statScale() * 10.0F);
        int affixes = data.affixes().size() * 6;
        int bulk = stack.getCount() > 1 ? stack.getCount() / 2 : 0;

        return Math.max(1, base + rarity + affixes + bulk);
    }

    /** What it costs to sweep the shelf and roll a new one. */
    public static int rerollCost(int rerollsThisCamp, int trainingLevel) {
        // Rising, so the shelf cannot simply be rerolled until it is perfect.
        return 6 + trainingLevel + rerollsThisCamp * 6;
    }

    public static void open(ServerPlayer player, TrainingSession session) {
        ShopStock stock = session.shopStock(player);

        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("container.smmorpg.shop");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inventory, Player who) {
                return new ShopMenu(id, inventory, session);
            }
        }, buf -> stock.write(buf));
    }
}
