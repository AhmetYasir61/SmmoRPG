package com.smmorpg.shop;

import com.smmorpg.core.ModMenus;
import com.smmorpg.training.CampServices;
import com.smmorpg.training.CampShop;
import com.smmorpg.training.TrainingSession;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The merchant's window.
 *
 * <p>The shelf is not an inventory you can reach into: the eight slots refuse every click
 * that would move a stack, and buying happens through the menu buttons instead. That is
 * what keeps the price honest — there is no interaction here that takes an item without
 * going past the till.
 */
public class ShopMenu extends AbstractContainerMenu {

    /** Button ids: 0..7 buy that slot, 8 rerolls the shelf. */
    public static final int REROLL = ShopStock.SIZE;

    private final Player player;
    private final TrainingSession session;
    private final Container shelf = new SimpleContainer(ShopStock.SIZE);

    private ShopStock stock;

    /** Client side: the shelf arrives as extra data because it is not a real container. */
    public ShopMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, (TrainingSession) null);
        this.stock = ShopStock.read(buf);
        fillShelf();
    }

    public ShopMenu(int id, Inventory inventory, TrainingSession session) {
        super(ModMenus.SHOP.get(), id);
        this.player = inventory.player;
        this.session = session;
        this.stock = ShopStock.EMPTY;

        for (int i = 0; i < ShopStock.SIZE; i++) {
            addSlot(new ShelfSlot(shelf, i, 26 + (i % 4) * 32, 30 + (i / 4) * 32));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 120 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 178));
        }

        if (session != null && player instanceof ServerPlayer sp) {
            this.stock = session.shopStock(sp);
            fillShelf();
        }
    }

    public ShopStock stock() { return stock; }

    public int rerollCost() {
        return session == null ? 0 : session.rerollCost();
    }

    private void fillShelf() {
        for (int i = 0; i < ShopStock.SIZE; i++) shelf.setItem(i, stock.good(i).copy());
        broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player who, int id) {
        if (!(who instanceof ServerPlayer sp) || session == null) return false;

        if (id == REROLL) {
            int cost = session.rerollCost();
            if (!CampServices.take(sp, cost)) return false;

            stock = session.rerollShop(sp);
            fillShelf();
            return true;
        }

        if (id < 0 || id >= ShopStock.SIZE) return false;

        ItemStack good = stock.good(id);
        if (good.isEmpty()) return false;

        if (!CampServices.take(sp, stock.price(id))) return false;

        ItemStack bought = good.copy();
        if (!sp.getInventory().add(bought) && !bought.isEmpty()) sp.drop(bought, false);

        stock = session.markSold(id);
        fillShelf();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player who, int index) {
        // Shift-clicking is how items normally escape a window without being paid for.
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player who) { return who.isAlive(); }

    /** A slot that shows a thing and refuses to hand it over. */
    private static final class ShelfSlot extends Slot {
        ShelfSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) { return false; }

        @Override
        public boolean mayPickup(Player player) { return false; }
    }
}
