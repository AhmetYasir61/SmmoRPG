package com.smmorpg.client.screen;

import com.smmorpg.coop.CoopGuest;
import com.smmorpg.coop.CoopHost;
import com.smmorpg.coop.RelayEndpoints;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;

/**
 * The co-op panel: hand out a code, or type one in.
 *
 * <p>Deliberately two halves of one screen rather than two screens. Hosting and joining
 * are the same social act from opposite ends, and someone who opens this because a friend
 * said "let's play" does not yet know which half they need.
 */
public class CoopScreen extends Screen {

    private final Screen parent;
    private EditBox codeBox;
    private Button hostButton;
    private Component status = Component.empty();

    public CoopScreen(Screen parent) {
        super(Component.translatable("coop.smmorpg.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 - 40;

        hostButton = addRenderableWidget(Button.builder(hostLabel(), b -> toggleHost())
                .bounds(cx - 100, y, 200, 20).build());

        codeBox = new EditBox(this.font, cx - 100, y + 60, 130, 20,
                Component.translatable("coop.smmorpg.code_hint"));
        codeBox.setMaxLength(8);
        codeBox.setHint(Component.translatable("coop.smmorpg.code_hint"));
        addRenderableWidget(codeBox);

        addRenderableWidget(Button.builder(Component.translatable("coop.smmorpg.join"), b -> join())
                .bounds(cx + 36, y + 60, 64, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> onClose())
                .bounds(cx - 100, y + 100, 200, 20).build());
    }

    private Component hostLabel() {
        return CoopHost.hosting()
                ? Component.translatable("coop.smmorpg.stop_hosting")
                : Component.translatable("coop.smmorpg.host");
    }

    private void toggleHost() {
        if (CoopHost.hosting()) {
            CoopHost.stop();
            status = Component.translatable("coop.smmorpg.stopped");
        } else if (minecraft == null || minecraft.getSingleplayerServer() == null) {
            // Hosting means sharing the world you are standing in; there is nothing
            // honest to share from the title screen.
            status = Component.translatable("coop.smmorpg.need_world");
        } else if (CoopHost.start() < 0) {
            status = Component.translatable("coop.smmorpg.host_failed");
        } else {
            status = Component.translatable("coop.smmorpg.hosting");
        }
        hostButton.setMessage(hostLabel());
    }

    private void join() {
        String code = codeBox.getValue().trim();
        if (code.length() < 4 || minecraft == null) {
            status = Component.translatable("coop.smmorpg.bad_code");
            return;
        }

        String address = CoopGuest.open(code);
        if (address == null) {
            status = Component.translatable("coop.smmorpg.join_failed");
            return;
        }

        // From here it is an ordinary connection, to an ordinary address, which happens to
        // be this machine. Everything after this is Minecraft's own joining code.
        ConnectScreen.startConnecting(new TitleScreen(), minecraft,
                ServerAddress.parseString(address),
                new ServerData("SmmoRPG co-op", address, ServerData.Type.OTHER),
                false, null);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g, mouseX, mouseY, partial);
        super.render(g, mouseX, mouseY, partial);

        int cx = this.width / 2;
        int top = this.height / 2 - 70;

        g.drawCenteredString(this.font, this.title, cx, top, 0xFFFFFF);

        String code = CoopHost.code();
        if (code != null) {
            g.drawCenteredString(this.font, Component.translatable("coop.smmorpg.your_code"),
                    cx, top + 46, 0xFFAAAAAA);
            g.drawCenteredString(this.font, code, cx, top + 58, 0xFFFFD760);
        }

        g.drawCenteredString(this.font, Component.translatable("coop.smmorpg.join_prompt"),
                cx, top + 82, 0xFFAAAAAA);

        if (!status.getString().isEmpty()) {
            g.drawCenteredString(this.font, status, cx, top + 152, 0xFF88CCFF);
        }
        if (!RelayEndpoints.configured()) {
            g.drawCenteredString(this.font, Component.translatable("coop.smmorpg.no_relay"),
                    cx, top + 166, 0xFFE08040);
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
