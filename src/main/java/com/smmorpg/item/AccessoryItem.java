package com.smmorpg.item;

import com.smmorpg.core.ModDataComponents;
import com.smmorpg.loot.GearData;
import com.smmorpg.loot.RolledAffix;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Rings, charms and talismans. They roll the same affix pool as weapons, so a cursed ring
 * is as much a decision as a cursed blade.
 */
public class AccessoryItem extends Item {
    public enum Slot { RING, AMULET, CHARM, TALISMAN }

    private final Slot slot;

    public AccessoryItem(Slot slot, Properties props) {
        super(props.stacksTo(1));
        this.slot = slot;
    }

    public Slot slot() { return slot; }

    /** Accessories live in the four dedicated slots of the RPG inventory screen. */
    public static boolean isAccessory(ItemStack stack) { return stack.getItem() instanceof AccessoryItem; }

    @Override
    public Component getName(ItemStack stack) {
        GearData data = stack.get(ModDataComponents.GEAR.get());
        Component base = super.getName(stack);
        return data == null ? base : Component.empty().append(base).withStyle(data.tier().color());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        GearData data = stack.get(ModDataComponents.GEAR.get());
        lines.add(Component.translatable("tooltip.smmorpg.slot." + slot.name().toLowerCase())
                .withStyle(ChatFormatting.DARK_AQUA));
        if (data == null) return;
        lines.add(Component.translatable(data.tier().translationKey()).withStyle(data.tier().color()));
        for (RolledAffix r : data.affixes()) {
            lines.add(Component.translatable(r.type().translationKey(), r.percent())
                    .withStyle(switch (r.type().alignment()) {
                        case HOLY -> ChatFormatting.YELLOW;
                        case CURSED -> ChatFormatting.DARK_RED;
                        case NEUTRAL -> ChatFormatting.GRAY;
                    }));
        }
    }
}
