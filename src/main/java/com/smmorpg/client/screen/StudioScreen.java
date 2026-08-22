package com.smmorpg.client.screen;

import com.smmorpg.network.C2SStudioEdit;
import com.smmorpg.studio.AnimationLibrary;
import com.smmorpg.studio.AnimationProfile;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * The operator's animation studio.
 *
 * <p>Every slider edits one field of one profile and sends it straight to the server, which
 * validates the permission, applies it and broadcasts it. The result is visible on the next
 * frame for everybody on the server — which is the point: movement is tuned by feel, and
 * feel needs the change to be instant.
 */
public class StudioScreen extends Screen {

    /** Fields exposed for editing, with their sane ranges. */
    private record Field(String key, float min, float max) {}

    private static final List<Field> FIELDS = List.of(
            new Field("duration", 1.0F, 60.0F),
            new Field("strike_at", 0.05F, 0.95F),
            new Field("arm_swing", 0.0F, 360.0F),
            new Field("body_twist", 0.0F, 90.0F),
            new Field("lean", 0.0F, 45.0F),
            new Field("recovery_ease", 0.2F, 4.0F),
            new Field("camera_kick", 0.0F, 2.0F));

    private final List<String> profileIds = new ArrayList<>(AnimationLibrary.all().keySet());
    private int selected = 0;

    public StudioScreen() {
        super(Component.translatable("studio.smmorpg.title"));
    }

    private AnimationProfile current() {
        return AnimationLibrary.get(profileIds.get(selected));
    }

    @Override
    protected void init() {
        int left = 12;
        int y = 34;
        for (int i = 0; i < profileIds.size(); i++) {
            int index = i;
            addRenderableWidget(Button.builder(
                            Component.translatable("studio.smmorpg.anim." + profileIds.get(i)),
                            b -> { selected = index; rebuildWidgets(); })
                    .bounds(left, y + i * 18, 120, 16).build());
        }

        AnimationProfile profile = current();
        int cx = this.width / 2 + 20;
        for (int i = 0; i < FIELDS.size(); i++) {
            Field field = FIELDS.get(i);
            float value = valueOf(profile, field.key());
            addRenderableWidget(new AbstractSliderButton(cx, 40 + i * 24, 180, 20,
                    Component.empty(), norm(value, field)) {
                { updateMessage(); }

                @Override
                protected void updateMessage() {
                    setMessage(Component.literal(String.format("%s: %.2f",
                            field.key(), denorm(this.value, field))));
                }

                @Override
                protected void applyValue() {
                    PacketDistributor.sendToServer(new C2SStudioEdit(
                            current().id(), field.key(), denorm(this.value, field)));
                }
            });
        }

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(this.width / 2 - 50, this.height - 28, 100, 20).build());
    }

    private static float valueOf(AnimationProfile p, String key) {
        return switch (key) {
            case "duration" -> p.durationTicks();
            case "strike_at" -> p.strikeAtFraction();
            case "arm_swing" -> p.armSwingDegrees();
            case "body_twist" -> p.bodyTwistDegrees();
            case "lean" -> p.leanDegrees();
            case "recovery_ease" -> p.recoveryEase();
            case "camera_kick" -> p.cameraKick();
            default -> 0.0F;
        };
    }

    private static double norm(float value, Field f) {
        return Math.max(0.0D, Math.min(1.0D, (value - f.min()) / (f.max() - f.min())));
    }

    private static float denorm(double value, Field f) {
        return (float) (f.min() + value * (f.max() - f.min()));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g, mouseX, mouseY, partial);
        super.render(g, mouseX, mouseY, partial);
        g.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        g.drawString(this.font, Component.translatable("studio.smmorpg.editing",
                        Component.translatable("studio.smmorpg.anim." + current().id())),
                this.width / 2 + 20, 24, 0xFFD760);
        g.drawString(this.font, Component.translatable("studio.smmorpg.live_note"),
                12, this.height - 44, 0x88CCFF);
    }
}
