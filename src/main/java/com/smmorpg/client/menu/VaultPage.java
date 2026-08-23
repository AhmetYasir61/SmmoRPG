package com.smmorpg.client.menu;

import com.smmorpg.account.PlayerAccount;
import com.smmorpg.account.VaultItem;
import com.smmorpg.vault.DurabilityRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Consumer;

/**
 * What the account is holding.
 *
 * <p>Wear is drawn on every slot, because the whole point of a vault that preserves damage
 * is that you can see what came back worn and decide what needs repairing before you take
 * it anywhere.
 */
public class VaultPage extends HubPage {

    private static final int COLUMNS = 12;
    private static final int SLOT = 20;

    private int scroll;
    private int selected = -1;

    @Override public Component title() { return Component.translatable("hub.smmorpg.vault"); }
    @Override public String icon() { return "▤"; }

    @Override
    protected void build(Consumer<AbstractWidget> register) {
        scroll = 0;
        selected = -1;

        register.accept(Button.builder(Component.literal("▲"), b -> scroll = Math.max(0, scroll - 1))
                .bounds(left + width - 18, top, 16, 16).build());
        register.accept(Button.builder(Component.literal("▼"), b -> scroll++)
                .bounds(left + width - 18, top + 18, 16, 16).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        var mc = Minecraft.getInstance();
        PlayerAccount account = ProfileCache.get();
        List<VaultItem> items = account.vault();

        if (items.isEmpty()) {
            g.drawString(mc.font, Component.translatable("hub.smmorpg.vault_empty"),
                    left, top + 8, 0x777F8C, false);
            return;
        }

        int rows = Math.max(1, (height - 34) / SLOT);
        int maxScroll = Math.max(0, (items.size() + COLUMNS - 1) / COLUMNS - rows);
        if (scroll > maxScroll) scroll = maxScroll;

        int first = scroll * COLUMNS;
        ItemStack hovered = ItemStack.EMPTY;

        for (int i = 0; i < rows * COLUMNS; i++) {
            int index = first + i;
            if (index >= items.size()) break;

            int x = left + (i % COLUMNS) * SLOT;
            int y = top + (i / COLUMNS) * SLOT;

            g.fill(x, y, x + SLOT - 2, y + SLOT - 2,
                    index == selected ? 0xFF3A4A6A : 0xFF1E1E26);

            ItemStack stack = items.get(index).toStack();
            g.renderItem(stack, x + 2, y + 2);
            g.renderItemDecorations(mc.font, stack, x + 2, y + 2);

            drawWear(g, stack, x + 2, y + 2);

            if (mouseX >= x && mouseX < x + SLOT - 2 && mouseY >= y && mouseY < y + SLOT - 2) {
                hovered = stack;
            }
        }

        g.drawString(mc.font, Component.translatable("hub.smmorpg.vault_count",
                        items.size()), left, top + height - 12, 0x777F8C, false);

        if (!hovered.isEmpty()) g.renderTooltip(mc.font, hovered, mouseX, mouseY);
    }

    /** A thin bar under a worn item, red as it approaches breaking. */
    private static void drawWear(GuiGraphics g, ItemStack stack, int x, int y) {
        float wear = DurabilityRules.wear(stack);
        if (wear <= 0.0F) return;

        int span = 16;
        int remaining = Math.round(span * (1.0F - wear));
        int colour = DurabilityRules.nearlyBroken(stack) ? 0xFFE04040
                : wear > 0.5F ? 0xFFE0C040 : 0xFF56D364;

        g.fill(x, y + 15, x + span, y + 16, 0xFF202020);
        g.fill(x, y + 15, x + remaining, y + 16, colour);
    }
}
