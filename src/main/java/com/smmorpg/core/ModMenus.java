package com.smmorpg.core;

import com.smmorpg.SmmoRPG;
import com.smmorpg.vault.VaultMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> REGISTRY =
            DeferredRegister.create(Registries.MENU, SmmoRPG.MOD_ID);

    /**
     * The vault window.
     *
     * <p>Registered with extra data because the client has to know which page it is looking
     * at and how many pages exist before it draws a single slot; a vault that silently
     * showed you page one of four would look like a vault that ate your things.
     */
    public static final Supplier<MenuType<VaultMenu>> VAULT = REGISTRY.register(
            "vault", () -> IMenuTypeExtension.create(VaultMenu::new));

    /**
     * The camp merchant's shelf.
     *
     * <p>Carries its goods as extra data rather than as a container, because the shelf is
     * not something the client is allowed to reach into — it is a picture of what is for
     * sale, and every actual exchange goes through a menu button the server rules on.
     */
    public static final Supplier<MenuType<com.smmorpg.shop.ShopMenu>> SHOP = REGISTRY.register(
            "shop", () -> IMenuTypeExtension.create(com.smmorpg.shop.ShopMenu::new));
}
