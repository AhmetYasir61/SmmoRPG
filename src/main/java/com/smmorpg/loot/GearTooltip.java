package com.smmorpg.loot;

import com.smmorpg.SmmoRPG;
import com.smmorpg.core.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Shows rolled affixes on whatever is carrying them.
 *
 * <p>An event rather than an override on our own item classes, because a rolled diamond
 * sword or a rolled Weapons of Miracles blade has to read the same as a rolled katana —
 * the roll lives on the stack, not on the item.
 */
@EventBusSubscriber(modid = SmmoRPG.MOD_ID)
public final class GearTooltip {

    private GearTooltip() {}

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        GearData data = stack.get(ModDataComponents.GEAR.get());
        if (data == null) return;

        var lines = event.getToolTip();
        lines.add(Component.translatable(data.tier().translationKey()).withStyle(data.tier().color()));
        lines.add(Component.translatable("tooltip.smmorpg.item_level", data.itemLevel())
                .withStyle(ChatFormatting.DARK_GRAY));

        for (RolledAffix rolled : data.affixes()) {
            lines.add(Component.translatable(rolled.type().translationKey(), rolled.percent())
                    .withStyle(switch (rolled.type().alignment()) {
                        case HOLY -> ChatFormatting.YELLOW;
                        case CURSED -> ChatFormatting.DARK_RED;
                        case NEUTRAL -> ChatFormatting.GRAY;
                    }));
        }

        if (data.cursed()) {
            lines.add(Component.translatable("tooltip.smmorpg.cursed_warning",
                    String.format("%.1f", data.lifeCostPerHit())).withStyle(ChatFormatting.DARK_RED));
        }
        if (data.powerOf(Affix.UNDEAD_BANE) > 0.0F) {
            lines.add(Component.translatable("tooltip.smmorpg.holy_blessing").withStyle(ChatFormatting.GOLD));
        }
    }
}
