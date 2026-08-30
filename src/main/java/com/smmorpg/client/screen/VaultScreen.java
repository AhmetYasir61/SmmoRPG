package com.smmorpg.client.screen;

import com.smmorpg.vault.VaultMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The vault window.
 *
 * <p>Drawn rather than textured: the two side slots need labels a chest texture has no
 * room for, and a player who cannot tell the deposit slot from the trash slot will
 * eventually put a sword in the wrong one.
 */
public class VaultScreen extends AbstractContainerScreen<VaultMenu> {

    private static final int PANEL = 0xF0100D14;
    private static final int EDGE = 0xFF3A3350;
    private static final int TRASH_EDGE = 0xFF8B2E2E;
    private static final int DEPOSIT_EDGE = 0xFF2E7D5B;

    private Button prev;
    private Button next;

    public VaultScreen(VaultMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 248;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        prev = addRenderableWidget(Button.builder(Component.literal("<"),
                        b -> clickButton(0))
                .bounds(leftPos + 30, topPos + 138, 20, 20).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"),
                        b -> clickButton(1))
                .bounds(leftPos + 126, topPos + 138, 20, 20).build());
        updatePageButtons();
    }

    private void clickButton(int id) {
        if (minecraft == null || minecraft.gameMode == null) return;
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        updatePageButtons();
    }

    private void updatePageButtons() {
        prev.active = menu.page() > 0;
        next.active = menu.page() < menu.pageCount() - 1;
        prev.visible = next.visible = menu.pageCount() > 1;
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        gfx.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
        gfx.renderOutline(leftPos, topPos, imageWidth, imageHeight, EDGE);

        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                slotFrame(gfx, leftPos + 7 + col * 18, topPos + 17 + row * 18, EDGE);
            }
        }

        slotFrame(gfx, leftPos + 7, topPos + 139, DEPOSIT_EDGE);
        slotFrame(gfx, leftPos + 151, topPos + 139, TRASH_EDGE);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                slotFrame(gfx, leftPos + 7 + col * 18, topPos + 165 + row * 18, EDGE);
            }
        }
        for (int col = 0; col < 9; col++) {
            slotFrame(gfx, leftPos + 7 + col * 18, topPos + 223, EDGE);
        }
    }

    private void slotFrame(GuiGraphics gfx, int x, int y, int colour) {
        gfx.fill(x, y, x + 18, y + 18, 0x40000000);
        gfx.renderOutline(x, y, 18, 18, colour);
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        super.renderLabels(gfx, mouseX, mouseY);

        gfx.drawString(font, Component.translatable("container.smmorpg.vault.deposit"),
                28, 131, 0xFF6FD3A6, false);
        gfx.drawString(font, Component.translatable("container.smmorpg.vault.trash"),
                104, 131, 0xFFD37070, false);

        if (menu.pageCount() > 1) {
            Component page = Component.translatable("container.smmorpg.vault.page",
                    menu.page() + 1, menu.pageCount());
            gfx.drawString(font, page, (imageWidth - font.width(page)) / 2, 145, 0xFFAAAAAA, false);
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        super.render(gfx, mouseX, mouseY, partialTick);
        updatePageButtons();
    }
}
