package com.smmorpg.client.screen;

import com.smmorpg.capability.PlayerProgress;
import com.smmorpg.client.ClientState;
import com.smmorpg.network.C2SSpendPoint;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Level, experience bar and the four stats you spend points into. */
public class CharacterScreen extends Screen {

    private static final String[] STATS = {"strength", "agility", "vitality", "spirit"};

    public CharacterScreen() {
        super(Component.translatable("screen.smmorpg.character"));
    }

    @Override
    protected void init() {
        int x = this.width / 2 + 60;
        int y = this.height / 2 - 30;
        for (int i = 0; i < STATS.length; i++) {
            String stat = STATS[i];
            addRenderableWidget(Button.builder(Component.literal("+"),
                            b -> PacketDistributor.sendToServer(new C2SSpendPoint(stat)))
                    .bounds(x, y + i * 20, 18, 18).build());
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g, mouseX, mouseY, partial);
        super.render(g, mouseX, mouseY, partial);

        PlayerProgress p = ClientState.progress;
        int left = this.width / 2 - 110;
        int y = this.height / 2 - 70;

        g.drawString(this.font, Component.translatable(p.playerClass().translationKey()), left, y, 0xFFD760);
        g.drawString(this.font, Component.translatable("screen.smmorpg.level", p.level()), left, y + 14, 0xFFFFFF);

        int need = p.xpForNext();
        float frac = need <= 0 ? 1.0F : (float) p.experience() / need;
        g.fill(left, y + 28, left + 200, y + 34, 0xFF202020);
        g.fill(left, y + 28, left + (int) (200 * Math.min(1.0F, frac)), y + 34, 0xFF55DD55);
        g.drawString(this.font, p.experience() + " / " + need, left, y + 38, 0x99FFFFFF);

        int statY = this.height / 2 - 30;
        int[] values = {p.strength(), p.agility(), p.vitality(), p.spirit()};
        for (int i = 0; i < STATS.length; i++) {
            g.drawString(this.font, Component.translatable("stat.smmorpg." + STATS[i]),
                    left, statY + i * 20 + 5, 0xCCCCCC);
            g.drawString(this.font, String.valueOf(values[i]), left + 110, statY + i * 20 + 5, 0xFFFFFF);
        }

        g.drawString(this.font, Component.translatable("screen.smmorpg.points", p.unspentPoints()),
                left, statY + 90, p.unspentPoints() > 0 ? 0xFFFF55 : 0x777777);
        g.drawString(this.font, String.format("DMG x%.2f   HP %.1f   STA %.0f",
                        p.meleeDamageMultiplier(), p.maxHealth(), p.staminaMax()),
                left, statY + 104, 0x88CCFF);
    }
}
