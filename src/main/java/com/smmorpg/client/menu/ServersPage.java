package com.smmorpg.client.menu;

import com.smmorpg.client.screen.ModServerListScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/** Where to go and play. Also the way into the training arena from the menu. */
public class ServersPage extends HubPage {

    @Override public Component title() { return Component.translatable("hub.smmorpg.play"); }
    @Override public String icon() { return "▶"; }

    @Override
    protected void build(Consumer<AbstractWidget> register) {
        register.accept(Button.builder(Component.translatable("servers.smmorpg.title"),
                        b -> Minecraft.getInstance().setScreen(new ModServerListScreen(hub)))
                .bounds(left, top, 180, 20).build());

        register.accept(Button.builder(Component.translatable("training.smmorpg.title"),
                        b -> Minecraft.getInstance().setScreen(
                                new com.smmorpg.client.screen.TrainingScreen()))
                .bounds(left, top + 26, 180, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        var font = Minecraft.getInstance().font;
        g.drawString(font, Component.translatable("hub.smmorpg.play_note"),
                left, top + 58, 0x777F8C, false);
    }
}
