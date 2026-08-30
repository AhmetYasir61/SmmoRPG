package com.smmorpg.client.screen;

import com.smmorpg.SmmoRPG;
import com.smmorpg.client.ClientNet;
import com.smmorpg.network.C2SCarriedAction;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * The vault and trash slots, added to the inventory you already open with E.
 *
 * <p>They live here rather than only in a separate vault window because this is where the
 * decision actually happens: you finish a fight, open your bag, and want the good sword
 * stored and the bent one gone without a second screen in between.
 *
 * <p>Each one acts on whatever is on your cursor. Nothing happens on an empty cursor, so
 * brushing past them costs nothing.
 */
@EventBusSubscriber(modid = SmmoRPG.MOD_ID, value = Dist.CLIENT)
public final class InventorySlotsOverlay {

    private static final int SIZE = 18;

    private InventorySlotsOverlay() {}

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) return;

        // To the right of the recipe-book edge, stacked, clear of every vanilla slot.
        int x = screen.getGuiLeft() + screen.getXSize() + 4;
        int y = screen.getGuiTop() + 8;

        event.addListener(new SlotButton(x, y, C2SCarriedAction.Action.DEPOSIT,
                "container.smmorpg.vault.deposit", 0xFF2E7D5B, "▼"));
        event.addListener(new SlotButton(x, y + SIZE + 4, C2SCarriedAction.Action.TRASH,
                "container.smmorpg.vault.trash", 0xFF8B2E2E, "✕"));
    }

    private static final class SlotButton extends AbstractButton {

        private final C2SCarriedAction.Action action;
        private final int edge;
        private final String glyph;

        SlotButton(int x, int y, C2SCarriedAction.Action action, String key, int edge, String glyph) {
            super(x, y, SIZE, SIZE, Component.translatable(key));
            this.action = action;
            this.edge = edge;
            this.glyph = glyph;
        }

        @Override
        public void onPress() {
            ClientNet.sendToServer(new C2SCarriedAction(action));
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            g.fill(getX(), getY(), getX() + width, getY() + height, 0xF0100D14);
            g.renderOutline(getX(), getY(), width, height, edge);

            var font = net.minecraft.client.Minecraft.getInstance().font;
            g.drawCenteredString(font, glyph, getX() + width / 2, getY() + 5, edge | 0xFF000000);

            if (isHovered()) {
                g.renderTooltip(font, getMessage(), mouseX, mouseY);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
