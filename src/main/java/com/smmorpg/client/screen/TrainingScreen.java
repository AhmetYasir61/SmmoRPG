package com.smmorpg.client.screen;

import com.smmorpg.client.ClientState;
import com.smmorpg.training.Difficulty;
import com.smmorpg.training.TrainingLevels;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The way into the arena.
 *
 * <p>There is nothing to set. The difficulty is whatever level you have already fought
 * your way to — a slider would let anyone put 100000% in on their first minute and learn
 * nothing from being erased by it. What this screen does instead is tell you exactly what
 * is waiting at your level before you walk into it.
 */
public class TrainingScreen extends Screen {

    public TrainingScreen() {
        super(Component.translatable("training.smmorpg.title"));
    }

    private static int level() {
        return ClientState.trainingLevel;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 + 30;

        addRenderableWidget(Button.builder(Component.translatable("training.smmorpg.enter"), b -> {
            // The launcher handles both cases: send it now if we are in a world, or open
            // the arena world first and hold the request until the player is actually in it.
            com.smmorpg.client.TrainingLauncher.enter();
            onClose();
        }).bounds(cx - 100, y, 96, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(cx + 4, y, 96, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g, mouseX, mouseY, partial);
        super.render(g, mouseX, mouseY, partial);

        int cx = this.width / 2;
        int top = this.height / 2 - 70;

        Difficulty d = TrainingLevels.difficultyFor(level());

        g.drawCenteredString(this.font, this.title, cx, top, 0xFFFFFF);
        g.drawCenteredString(this.font,
                Component.translatable("training.smmorpg.level", level(), d.percent()),
                cx, top + 18, 0xFFFFFF);
        g.drawCenteredString(this.font, Component.translatable(d.tierKey()),
                cx, top + 32, d.divine() ? 0xFFAA00 : 0xAAAAAA);

        String line = String.format("HP x%.1f   DMG x%.1f   SPD x%.2f   REACT %dt   PARRY %d%%   FOES %d",
                d.healthMultiplier(), d.damageMultiplier(), d.speedMultiplier(),
                d.reactionTicks(), Math.round(d.parryChance() * 100), d.simultaneousOpponents());
        g.drawCenteredString(this.font, line, cx, top + 52, 0x88CCFF);

        g.drawCenteredString(this.font,
                Component.translatable("training.smmorpg.wave_size", TrainingLevels.killsFor(level())),
                cx, top + 66, 0xAAAAAA);
        g.drawCenteredString(this.font, Component.translatable("training.smmorpg.camp_hint"),
                cx, top + 82, 0x88AA88);

        int next = TrainingLevels.percentFor(level() + 1);
        if (next > d.percent()) {
            g.drawCenteredString(this.font,
                    Component.translatable("training.smmorpg.next_level", level() + 1, next),
                    cx, top + 96, 0x777777);
        }
    }
}
