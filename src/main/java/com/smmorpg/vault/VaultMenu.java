package com.smmorpg.vault;

import com.smmorpg.account.PlayerAccount;
import com.smmorpg.account.VaultItem;
import com.smmorpg.backend.AccountService;
import com.smmorpg.core.ModMenus;
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
 * The vault as a window you can reach into.
 *
 * <p>The vault itself lives on the account service, not in this world, so this menu is a
 * view onto it rather than a chest that holds anything. Every take and every deposit is
 * applied to the account immediately and the view is rebuilt from what the account then
 * says — which means the window can never drift from the truth, even if two of them are
 * open at once.
 *
 * <p>Taking and putting are deliberately different actions. The grid is read-only: you
 * pull things out of it. Putting something in goes through the single deposit slot, one
 * stack at a time, because a vault you can dump your whole inventory into with one
 * shift-click is a vault you will empty by accident.
 */
public class VaultMenu extends AbstractContainerMenu {

    /** One screen of vault. Six rows is as much as fits above an inventory. */
    public static final int PAGE_SIZE = 54;

    private static final int DEPOSIT = 0;
    private static final int TRASH = 1;

    private final Player player;
    private final Container grid = new SimpleContainer(PAGE_SIZE);
    private final Container sideSlots = new SimpleContainer(2);

    private int page;
    private int pageCount;

    /** True while this menu is writing to its own containers, so it does not react to itself. */
    private boolean rebuilding;

    /** Client side: the server has already told us the page and how many there are. */
    public VaultMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readVarInt(), buf.readVarInt());
    }

    public VaultMenu(int id, Inventory inventory, int page, int pageCount) {
        super(ModMenus.VAULT.get(), id);
        this.player = inventory.player;
        this.page = page;
        this.pageCount = pageCount;

        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new VaultSlot(grid, row * 9 + col, 8 + col * 18, 18 + row * 18));
            }
        }

        addSlot(new Slot(sideSlots, DEPOSIT, 8, 140));
        addSlot(new Slot(sideSlots, TRASH, 152, 140));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 166 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 224));
        }

        refresh();
    }

    public int page() { return page; }
    public int pageCount() { return pageCount; }

    // --- the account behind the window ---

    private PlayerAccount account() {
        return player instanceof ServerPlayer sp ? AccountService.of(sp) : null;
    }

    private void commit(PlayerAccount next) {
        AccountService.put(next);
        refresh();
    }

    /** Rebuilds the visible page from the account. The account is always the truth. */
    private void refresh() {
        PlayerAccount account = account();
        if (account == null) return;                 // client: contents arrive by sync

        rebuilding = true;
        try {
            pageCount = Math.max(1, (account.vault().size() + PAGE_SIZE - 1) / PAGE_SIZE);
            page = Math.min(page, pageCount - 1);

            for (int i = 0; i < PAGE_SIZE; i++) {
                int index = page * PAGE_SIZE + i;
                grid.setItem(i, index < account.vault().size()
                        ? account.vault().get(index).toStack()
                        : ItemStack.EMPTY);
            }
        } finally {
            rebuilding = false;
        }
        broadcastChanges();
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (rebuilding || container != sideSlots) return;

        ItemStack deposit = sideSlots.getItem(DEPOSIT);
        if (deposit.isEmpty()) return;

        PlayerAccount account = account();
        if (account == null) return;

        sideSlots.setItem(DEPOSIT, ItemStack.EMPTY);
        commit(account.deposit(VaultItem.of(deposit)));
    }

    @Override
    public boolean clickMenuButton(Player who, int id) {
        int next = switch (id) {
            case 0 -> page - 1;
            case 1 -> page + 1;
            default -> page;
        };
        if (next < 0 || next >= pageCount || next == page) return false;
        page = next;
        refresh();
        return true;
    }

    @Override
    public void removed(Player who) {
        // Whatever is sitting in the trash slot when the window closes is gone. Deleting on
        // placement would punish a misclick with no way back; this way the slot is a
        // decision you can still undo right up until you walk away from it.
        sideSlots.setItem(TRASH, ItemStack.EMPTY);
        super.removed(who);
    }

    @Override
    public ItemStack quickMoveStack(Player who, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();

        if (index < PAGE_SIZE) {
            // Out of the vault and into the bag; the slot's own onTake books the withdrawal.
            int before = stack.getCount();
            if (!moveItemStackTo(stack, PAGE_SIZE + 2, slots.size(), true)) return ItemStack.EMPTY;

            // Only what actually fit in the bag leaves the vault. Booking the whole stack
            // when half of it bounced would quietly delete the half still on the shelf.
            int moved = before - stack.getCount();
            if (moved > 0) slot.onTake(who, stack.copyWithCount(moved));
            return ItemStack.EMPTY;
        }

        if (index >= PAGE_SIZE + 2) {
            // Shift-clicking from the bag deposits exactly the one stack you clicked.
            PlayerAccount account = account();
            if (account == null) return ItemStack.EMPTY;
            commit(account.deposit(VaultItem.of(stack)));
            slot.set(ItemStack.EMPTY);
            return ItemStack.EMPTY;
        }

        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player who) {
        return who.isAlive();
    }

    /**
     * A slot you can only take from.
     *
     * <p>The grid mirrors the account, so anything placed here would be a change the
     * account never heard about — it would sit there looking stored until the next refresh
     * quietly erased it.
     */
    private final class VaultSlot extends Slot {
        VaultSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) { return false; }

        @Override
        public void onTake(Player who, ItemStack taken) {
            PlayerAccount account = account();
            if (account != null) {
                commit(account.withdraw(page * PAGE_SIZE + getSlotIndex(), taken.getCount()));
            }
            super.onTake(who, taken);
        }
    }
}
