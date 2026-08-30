package com.smmorpg.client.screen;

import com.smmorpg.shop.ShopMenu;
import com.smmorpg.shop.ShopStock;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The merchant's shelf, with the price written under every item.
 *
 * <p>Buying is a button rather than a drag, because a shelf you can drag from is a shelf
 * someone will eventually take from by accident. Under the eight goods sits the reroll,
 * with its own rising price on it so the choice between "buy this" and "see something
 * else" is always in front of you as a number.
 */
public class ShopScreen extends AbstractContainerScreen<ShopMenu> {

    private static final int PANEL = 0xF0100D14;
    private static final int EDGE = 0xFF3A3350;
    private static final int GOLD = 0xFFD7A560;

    private final Button[] buy = new Button[ShopStock.SIZE];
    private Button reroll;

    public ShopScreen(ShopMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 202;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        for (int i = 0; i < ShopStock.SIZE; i++) {
            int index = i;
            buy[i] = addRenderableWidget(Button.builder(Component.literal("•"),
                            b -> click(index))
                    .bounds(leftPos + 26 + (i % 4) * 32, topPos + 48 + (i / 4) * 32, 20, 12)
                    .build());
        }

        reroll = addRenderableWidget(Button.builder(Component.empty(), b -> click(ShopMenu.REROLL))
                .bounds(leftPos + 26, topPos + 96, 124, 18).build());

        refresh();
    }

    private void click(int id) {
        if (minecraft == null || minecraft.gameMode == null) return;
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        refresh();
    }

    private void refresh() {
        ShopStock stock = menu.stock();
        for (int i = 0; i < buy.length; i++) {
            ItemStack good = stock.good(i);
            buy[i].active = !good.isEmpty();
            buy[i].setMessage(good.isEmpty()
                    ? Component.literal("—")
                    : Component.literal(String.valueOf(stock.price(i))).withStyle(
                            net.minecraft.ChatFormatting.GREEN));
        }
        reroll.setMessage(Component.translatable("shop.smmorpg.reroll", menu.rerollCost()));
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        gfx.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
        gfx.renderOutline(leftPos, topPos, imageWidth, imageHeight, EDGE);

        for (int i = 0; i < ShopStock.SIZE; i++) {
            int sx = leftPos + 25 + (i % 4) * 32;
            int sy = topPos + 29 + (i / 4) * 32;
            gfx.fill(sx, sy, sx + 18, sy + 18, 0x40000000);
            gfx.renderOutline(sx, sy, 18, 18, EDGE);
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                slot(gfx, leftPos + 7 + col * 18, topPos + 119 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            slot(gfx, leftPos + 7 + col * 18, topPos + 177);
        }
    }

    private void slot(GuiGraphics gfx, int x, int y) {
        gfx.fill(x, y, x + 18, y + 18, 0x40000000);
        gfx.renderOutline(x, y, 18, 18, EDGE);
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        super.renderLabels(gfx, mouseX, mouseY);

        int purse = purse();
        gfx.renderItem(new ItemStack(Items.EMERALD), 8, 14);
        gfx.drawString(font, String.valueOf(purse), 28, 18, GOLD, false);
        gfx.drawString(font, Component.translatable("shop.smmorpg.purse"), 46, 18, 0xFF888888, false);
    }

    /** What the player can actually spend, counted from their own inventory. */
    private int purse() {
        if (minecraft == null || minecraft.player == null) return 0;
        int total = 0;
        for (ItemStack stack : minecraft.player.getInventory().items) {
            if (stack.is(Items.EMERALD)) total += stack.getCount();
        }
        return total;
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        super.render(gfx, mouseX, mouseY, partialTick);
        refresh();
    }
}
