package com.smmorpg.client.screen;

import com.smmorpg.training.Difficulty;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Picks the training difficulty before dropping into the arena.
 *
 * <p>The slider is logarithmic, because a linear 1..100000 control would spend almost all
 * of its travel above 10000 and make the useful low range unusable.
 */
public class TrainingScreen extends Screen {

    private int percent = 100;
    private EditBox exact;

    public TrainingScreen() {
        super(Component.translatable("training.smmorpg.title"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 - 20;

        addRenderableWidget(new AbstractSliderButton(cx - 100, y, 200, 20,
                Component.empty(), toSlider(percent)) {
            { updateMessage(); }

            @Override
            protected void updateMessage() {
                setMessage(Component.translatable("training.smmorpg.difficulty", percent));
            }

            @Override
            protected void applyValue() {
                percent = fromSlider(this.value);
                if (exact != null) exact.setValue(String.valueOf(percent));
            }
        });

        exact = new EditBox(this.font, cx - 50, y + 26, 100, 18,
                Component.translatable("training.smmorpg.exact"));
        exact.setValue(String.valueOf(percent));
        exact.setResponder(text -> {
            try {
                percent = new Difficulty(Integer.parseInt(text.trim())).percent();
            } catch (NumberFormatException ignored) {
                // Half-typed numbers are normal; keep the last valid value.
            }
        });
        addRenderableWidget(exact);

        addRenderableWidget(Button.builder(Component.translatable("training.smmorpg.enter"), b -> {
            // The launcher handles both cases: send it now if we are in a world, or open
            // the arena world first and hold the request until the player is actually in it.
            com.smmorpg.client.TrainingLauncher.enter(percent);
            onClose();
        }).bounds(cx - 100, y + 52, 96, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(cx + 4, y + 52, 96, 20).build());
    }

    /** 1..100000 mapped onto 0..1 by log, so the low end gets real slider travel. */
    private static double toSlider(int percent) {
        return Math.log10(percent) / Math.log10(Difficulty.MAX);
    }

    private static int fromSlider(double value) {
        return new Difficulty((int) Math.round(Math.pow(Difficulty.MAX, value))).percent();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g, mouseX, mouseY, partial);
        super.render(g, mouseX, mouseY, partial);

        Difficulty d = new Difficulty(percent);
        int cx = this.width / 2;
        g.drawCenteredString(this.font, this.title, cx, this.height / 2 - 60, 0xFFFFFF);
        g.drawCenteredString(this.font, Component.translatable(d.tierKey()),
                cx, this.height / 2 - 44, d.divine() ? 0xFFAA00 : 0xAAAAAA);

        String line = String.format("HP x%.1f   DMG x%.1f   SPD x%.2f   REACT %dt   PARRY %d%%   FOES %d",
                d.healthMultiplier(), d.damageMultiplier(), d.speedMultiplier(),
                d.reactionTicks(), Math.round(d.parryChance() * 100), d.simultaneousOpponents());
        g.drawCenteredString(this.font, line, cx, this.height / 2 + 78, 0x88CCFF);

        if (d.divine()) {
            g.drawCenteredString(this.font, Component.translatable("training.smmorpg.band", d.band()),
                    cx, this.height / 2 + 92, 0xFF7744);
        }
    }
}
