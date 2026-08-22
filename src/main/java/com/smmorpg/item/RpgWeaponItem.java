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

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        GearData data = stack.get(ModDataComponents.GEAR.get());
        if (data == null) return;

        lines.add(Component.translatable(data.tier().translationKey()).withStyle(data.tier().color()));
        lines.add(Component.translatable("tooltip.smmorpg.item_level", data.itemLevel())
                .withStyle(ChatFormatting.DARK_GRAY));

        for (RolledAffix r : data.affixes()) {
            ChatFormatting color = switch (r.type().alignment()) {
                case HOLY -> ChatFormatting.YELLOW;
                case CURSED -> ChatFormatting.DARK_RED;
                case NEUTRAL -> ChatFormatting.GRAY;
            };
            lines.add(Component.translatable(r.type().translationKey(), r.percent()).withStyle(color));
        }

        if (data.cursed()) {
            lines.add(Component.translatable("tooltip.smmorpg.cursed_warning",
                    String.format("%.1f", data.lifeCostPerHit())).withStyle(ChatFormatting.DARK_RED));
        }
        if (data.powerOf(Affix.UNDEAD_BANE) > 0) {
            lines.add(Component.translatable("tooltip.smmorpg.holy_blessing").withStyle(ChatFormatting.GOLD));
        }
    }
}
