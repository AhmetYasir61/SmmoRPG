package com.smmorpg.vault;

import com.smmorpg.account.PlayerAccount;
import com.smmorpg.backend.AccountService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Opens the vault for a player, handing the client the page it is about to draw. */
public final class VaultOpener {

    private VaultOpener() {}

    public static void open(ServerPlayer player) {
        PlayerAccount account = AccountService.of(player);
        int pages = Math.max(1, (account.vault().size() + VaultMenu.PAGE_SIZE - 1) / VaultMenu.PAGE_SIZE);

        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("container.smmorpg.vault");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inventory, Player who) {
                return new VaultMenu(id, inventory, 0, pages);
            }
        }, buf -> {
            buf.writeVarInt(0);
            buf.writeVarInt(pages);
        });
    }
}
