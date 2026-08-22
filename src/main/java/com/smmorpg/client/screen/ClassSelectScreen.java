package com.smmorpg.client.screen;

import com.smmorpg.classes.PlayerClass;
import com.smmorpg.network.C2SChooseClass;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import com.smmorpg.client.ClientNet;
import net.minecraft.network.chat.Component;

/**
 * The first thing a new character sees. Deliberately unskippable: the class is the run,
 * and the screen refuses to close until one is picked.
 */
public class ClassSelectScreen extends Screen {

    private PlayerClass selected = PlayerClass.SHINOBI;

    public ClassSelectScreen() {
        super(Component.translatable("screen.smmorpg.choose_class"));
    }

    @Override
    protected void init() {
        PlayerClass[] classes = PlayerClass.values();
        int cols = 4;
        int cellW = 96, cellH = 24;
        int gridW = cols * cellW + (cols - 1) * 6;
        int startX = (this.width - gridW) / 2;
        int startY = this.height / 2 - 50;

        for (int i = 0; i < classes.length; i++) {
            PlayerClass pc = classes[i];
            int x = startX + (i % cols) * (cellW + 6);
            int y = startY + (i / cols) * (cellH + 6);
            addRenderableWidget(Button.builder(Component.translatable(pc.translationKey()),
                            b -> selected = pc)
                    .bounds(x, y, cellW, cellH).build());
        }

        addRenderableWidget(Button.builder(Component.translatable("screen.smmorpg.confirm"), b -> {
            ClientNet.sendToServer(new C2SChooseClass(selected.key()));
            onClose();
        }).bounds(this.width / 2 - 60, startY + 2 * (cellH + 6) + 46, 120, 22).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g, mouseX, mouseY, partial);
        super.render(g, mouseX, mouseY, partial);

        g.drawCenteredString(this.font, this.title, this.width / 2, 28, 0xFFFFFF);

        int infoY = this.height / 2 + 18;
        g.drawCenteredString(this.font,
                Component.translatable(selected.translationKey()).withStyle(ChatFormatting.GOLD),
                this.width / 2, infoY, 0xFFD760);
        g.drawCenteredString(this.font,
                Component.translatable(selected.descriptionKey()).withStyle(ChatFormatting.GRAY),
                this.width / 2, infoY + 12, 0xAAAAAA);

        String stats = String.format("HP %.0f  ATK x%.2f  SPD x%.2f  DEF x%.2f  PARRY %dt",
                selected.baseHealth(), selected.damageMultiplier(), selected.attackSpeedMultiplier(),
                selected.defenseMultiplier(), selected.parryWindowTicks());
        g.drawCenteredString(this.font, stats, this.width / 2, infoY + 26, 0x88CCFF);
        g.drawCenteredString(this.font,
                Component.translatable("screen.smmorpg.signature",
                        Component.translatable("ability.smmorpg." + selected.signatureAbility())),
                this.width / 2, infoY + 38, 0xFF9955);
    }

    /** The choice is permanent, so there is no escaping this screen. */
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return false; }
}
