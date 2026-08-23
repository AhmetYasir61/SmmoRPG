package com.smmorpg.item;

import com.smmorpg.core.ModDataComponents;
import com.smmorpg.loot.Affix;
import com.smmorpg.loot.GearData;
import com.smmorpg.loot.RolledAffix;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** A weapon whose real numbers come from its rolled {@link GearData}, not from the item id. */
public class RpgWeaponItem extends Item {
    private final WeaponClass weaponClass;

    public RpgWeaponItem(WeaponClass weaponClass, Properties props) {
        super(props);
        this.weaponClass = weaponClass;
    }

    public WeaponClass weaponClass() { return weaponClass; }

    public static WeaponClass classOf(ItemStack stack) {
        return stack.getItem() instanceof RpgWeaponItem w ? w.weaponClass() : null;
    }

    @Override
    public Component getName(ItemStack stack) {
        GearData data = stack.get(ModDataComponents.GEAR.get());
        Component base = super.getName(stack);
        if (data == null) return base;
        return Component.empty().append(base).withStyle(data.tier().color());
    }

}
