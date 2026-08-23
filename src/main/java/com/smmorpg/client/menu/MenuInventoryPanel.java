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
 * The inventory column on the right of the front page.
 *
 * <p>Two slots sit apart from the grid, as marked on the sketch: a trash slot and a vault
 * slot. Anything dropped on the vault slot goes to storage one stack at a time; anything
 * dropped on the trash is gone. Keeping them off the grid rather than in a corner of it is
 * deliberate — a destructive slot that looks like an ordinary slot is a slot people lose
 * things to.
 */
public class MenuInventoryPanel {

    private static final int SLOT = 20;
    private static final int SCROLLBAR = 6;

    private int x, y, width, height;
    private int columns;
    private int rows;
    private int scroll;
    private int selected = -1;

    /** Which loadout the arrows under the character are cycling. */
    private int loadout;

    public void layout(int x, int y, int width, int height, Consumer<AbstractWidget> register) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.columns = Math.max(4, (width - SCROLLBAR - 4) / SLOT);
        // The grid takes the top third; the panel below it is where item detail goes.
        this.rows = Math.max(2, (height / 3) / SLOT);
        this.scroll = 0;

        register.accept(Button.builder(Component.literal("▲"), b -> scroll = Math.max(0, scroll - 1))
                .bounds(x + width - SCROLLBAR - 12, y, 12, 12).build());
        register.accept(Button.builder(Component.literal("▼"), b -> scroll++)
                .bounds(x + width - SCROLLBAR - 12, y + rows * SLOT - 12, 12, 12).build());
    }

    public void cycle(int direction) {
        loadout = Math.floorMod(loadout + direction, 4);
    }

    public int loadout() { return loadout; }

    public void render(GuiGraphics g, int mouseX, int mouseY) {
        var mc = Minecraft.getInstance();
        PlayerAccount account = ProfileCache.get();
        List<VaultItem> items = account.vault();

        g.drawString(mc.font, Component.translatable("menu.smmorpg.inventory"),
                x, y - 12, 0xFFD760, false);

        int gridH = rows * SLOT;
        g.fill(x, y, x + width, y + gridH, MainMenuScreen.panelColour());

        int maxScroll = Math.max(0, (items.size() + columns - 1) / columns - rows);
        if (scroll > maxScroll) scroll = maxScroll;

        ItemStack hovered = ItemStack.EMPTY;
        int first = scroll * columns;

        for (int i = 0; i < rows * columns; i++) {
            int index = first + i;
            int sx = x + (i % columns) * SLOT;
            int sy = y + (i / columns) * SLOT;

            g.fill(sx + 1, sy + 1, sx + SLOT - 1, sy + SLOT - 1,
                    index == selected ? 0xFF3A4A6A : 0xFF1B1B22);

            if (index >= items.size()) continue;

            ItemStack stack = items.get(index).toStack();
            g.renderItem(stack, sx + 2, sy + 2);
            g.renderItemDecorations(mc.font, stack, sx + 2, sy + 2);
            drawWear(g, stack, sx + 2, sy + 2);

            if (mouseX >= sx && mouseX < sx + SLOT && mouseY >= sy && mouseY < sy + SLOT) {
                hovered = stack;
            }
        }

        drawScrollbar(g, gridH, maxScroll);
        drawSpecialSlots(g, mc, mouseX, mouseY);
        drawDetail(g, mc, gridH, items);

        if (!hovered.isEmpty()) g.renderTooltip(mc.font, hovered, mouseX, mouseY);
    }

    private void drawScrollbar(GuiGraphics g, int gridH, int maxScroll) {
        int barX = x + width - SCROLLBAR;
        g.fill(barX, y, barX + SCROLLBAR, y + gridH, 0xFF15151B);

        int thumbH = maxScroll == 0 ? gridH : Math.max(10, gridH / (maxScroll + 1));
        int travel = gridH - thumbH;
        int thumbY = y + (maxScroll == 0 ? 0 : travel * scroll / maxScroll);
        g.fill(barX, thumbY, barX + SCROLLBAR, thumbY + thumbH, 0xFFC03AA0);
    }

    /** The trash and the vault, drawn to the left of the grid where the sketch marks them. */
    private void drawSpecialSlots(GuiGraphics g, Minecraft mc, int mouseX, int mouseY) {
        int slotX = x - SLOT - 6;

        drawMarker(g, mc, slotX, y, "✕", 0xFFE04040,
                Component.translatable("menu.smmorpg.trash"), mouseX, mouseY);
        drawMarker(g, mc, slotX, y + SLOT + 4, "◍", 0xFF56D364,
                Component.translatable("menu.smmorpg.deposit"), mouseX, mouseY);
    }

    private void drawMarker(GuiGraphics g, Minecraft mc, int sx, int sy, String glyph,
                            int colour, Component tooltip, int mouseX, int mouseY) {
        g.fill(sx, sy, sx + SLOT, sy + SLOT, 0xFF1B1B22);
        g.fill(sx, sy, sx + SLOT, sy + 1, colour);
        g.fill(sx, sy + SLOT - 1, sx + SLOT, sy + SLOT, colour);
        g.drawCenteredString(mc.font, glyph, sx + SLOT / 2, sy + 6, colour);

        if (mouseX >= sx && mouseX < sx + SLOT && mouseY >= sy && mouseY < sy + SLOT) {
            g.renderTooltip(mc.font, tooltip, mouseX, mouseY);
        }
    }

    private void drawDetail(GuiGraphics g, Minecraft mc, int gridH, List<VaultItem> items) {
        int panelY = y + gridH + 8;
        int panelH = height - gridH - 8;
        if (panelH <= 20) return;

        g.fill(x, panelY, x + width, panelY + panelH, MainMenuScreen.panelColour());

        g.drawString(mc.font, Component.translatable("hub.smmorpg.vault_count", items.size()),
                x + 6, panelY + 6, MainMenuScreen.dimText(), false);

        g.drawString(mc.font, Component.translatable("menu.smmorpg.loadout", loadout + 1),
                x + 6, panelY + 20, 0xFFD760, false);

        if (ProfileCache.stale()) {
            g.drawString(mc.font, Component.translatable("hub.smmorpg.offline"),
                    x + 6, panelY + panelH - 14, 0xFF8855, false);
        }
    }

    private static void drawWear(GuiGraphics g, ItemStack stack, int px, int py) {
        float wear = DurabilityRules.wear(stack);
        if (wear <= 0.0F) return;

        int remaining = Math.round(16 * (1.0F - wear));
        int colour = DurabilityRules.nearlyBroken(stack) ? 0xFFE04040
                : wear > 0.5F ? 0xFFE0C040 : 0xFF56D364;

        g.fill(px, py + 15, px + 16, py + 16, 0xFF202020);
        g.fill(px, py + 15, px + remaining, py + 16, colour);
    }
}
