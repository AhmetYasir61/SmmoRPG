package com.smmorpg.core;

import com.smmorpg.SmmoRPG;
import com.smmorpg.item.AccessoryItem;
import com.smmorpg.item.RpgWeaponItem;
import com.smmorpg.item.WeaponClass;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

public final class ModItems {
    public static final DeferredRegister<Item> REGISTRY =
            DeferredRegister.create(Registries.ITEM, SmmoRPG.MOD_ID);

    public static final List<DeferredHolder<Item, ? extends Item>> ALL = new ArrayList<>();

    public static final DeferredHolder<Item, RpgWeaponItem> KATANA = weapon(WeaponClass.KATANA);
    public static final DeferredHolder<Item, RpgWeaponItem> ODACHI = weapon(WeaponClass.ODACHI);
    public static final DeferredHolder<Item, RpgWeaponItem> DAO = weapon(WeaponClass.DAO);
    public static final DeferredHolder<Item, RpgWeaponItem> JIAN = weapon(WeaponClass.JIAN);
    public static final DeferredHolder<Item, RpgWeaponItem> DAGGER = weapon(WeaponClass.DAGGER);
    public static final DeferredHolder<Item, RpgWeaponItem> TANTO = weapon(WeaponClass.TANTO);
    public static final DeferredHolder<Item, RpgWeaponItem> SPEAR = weapon(WeaponClass.SPEAR);
    public static final DeferredHolder<Item, RpgWeaponItem> NAGINATA = weapon(WeaponClass.NAGINATA);
    public static final DeferredHolder<Item, RpgWeaponItem> KANABO = weapon(WeaponClass.KANABO);
    public static final DeferredHolder<Item, RpgWeaponItem> YUMI = weapon(WeaponClass.BOW);

    public static final DeferredHolder<Item, AccessoryItem> RING = accessory("ring", AccessoryItem.Slot.RING);
    public static final DeferredHolder<Item, AccessoryItem> AMULET = accessory("amulet", AccessoryItem.Slot.AMULET);
    public static final DeferredHolder<Item, AccessoryItem> OMAMORI = accessory("omamori", AccessoryItem.Slot.CHARM);
    public static final DeferredHolder<Item, AccessoryItem> TALISMAN = accessory("talisman", AccessoryItem.Slot.TALISMAN);

    /** Consumables used by the wound system. */
    public static final DeferredHolder<Item, Item> BANDAGE =
            track(REGISTRY.register("bandage", () -> new Item(new Item.Properties().stacksTo(16))));
    public static final DeferredHolder<Item, Item> CAUTERY_IRON =
            track(REGISTRY.register("cautery_iron", () -> new Item(new Item.Properties().durability(64))));
    public static final DeferredHolder<Item, Item> BLOOD_VIAL =
            track(REGISTRY.register("blood_vial", () -> new Item(new Item.Properties().stacksTo(16))));

    private static DeferredHolder<Item, RpgWeaponItem> weapon(WeaponClass wc) {
        return track(REGISTRY.register(wc.key(),
                () -> new RpgWeaponItem(wc, new Item.Properties().stacksTo(1).durability(780))));
    }

    private static DeferredHolder<Item, AccessoryItem> accessory(String name, AccessoryItem.Slot slot) {
        return track(REGISTRY.register(name, () -> new AccessoryItem(slot, new Item.Properties())));
    }

    private static <T extends Item> DeferredHolder<Item, T> track(DeferredHolder<Item, T> h) {
        ALL.add(h);
        return h;
    }
}
