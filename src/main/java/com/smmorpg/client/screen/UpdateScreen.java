package com.smmorpg.client.screen;

import com.smmorpg.update.UpdateManifest;
import com.smmorpg.update.UpdateService;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLPaths;

/** Update, or wait. Both are real answers, and waiting never blocks play. */
public class UpdateScreen extends Screen {

    private final UpdateManifest manifest;
    private final Screen parent;

    private String status = "";
    private boolean working = false;
    private boolean staged = false;

    public UpdateScreen(UpdateManifest manifest, Screen parent) {
        super(Component.translatable("update.smmorpg.title"));
        this.manifest = manifest;
        this.parent = parent;
    }

    @Override
    protected void init() {
        int y = this.height / 2 + 24;

        addRenderableWidget(Button.builder(Component.translatable("update.smmorpg.update_now"), b -> {
            if (working) return;
            working = true;
            status = Component.translatable("update.smmorpg.downloading").getString();
            UpdateService.download(FMLPaths.MODSDIR.get()).thenAccept(ok ->
                    Minecraft.getInstance().execute(() -> {
                        working = false;
                        staged = ok;
                        status = Component.translatable(ok
                                ? "update.smmorpg.staged" : "update.smmorpg.failed").getString();
                        rebuildWidgets();
                    }));
        }).bounds(this.width / 2 - 154, y, 150, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("update.smmorpg.later"),
                        b -> onClose())
                .bounds(this.width / 2 + 4, y, 150, 20).build());

        if (staged) {
            // The jar is in place but not loaded: only a restart can pick it up.
            addRenderableWidget(Button.builder(Component.translatable("update.smmorpg.quit_to_apply"),
                            b -> Minecraft.getInstance().stop())
                    .bounds(this.width / 2 - 75, y + 26, 150, 20).build());
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g, mouseX, mouseY, partial);
        super.render(g, mouseX, mouseY, partial);

        g.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 60, 0xFFFFFF);
        g.drawCenteredString(this.font, Component.translatable("update.smmorpg.version_line",
                        com.smmorpg.update.UpdateClient.currentVersion(), manifest.version()),
                this.width / 2, this.height / 2 - 40, 0xFFD760);

        if (!manifest.notes().isBlank()) {
            g.drawCenteredString(this.font, manifest.notes(), this.width / 2,
                    this.height / 2 - 24, 0xAAAAAA);
        }
        if (manifest.mandatory()) {
            g.drawCenteredString(this.font,
                    Component.translatable("update.smmorpg.mandatory").withStyle(ChatFormatting.RED),
                    this.width / 2, this.height / 2 - 8, 0xFF5555);
        }
        if (!status.isEmpty()) {
            g.drawCenteredString(this.font, status, this.width / 2, this.height / 2 + 6, 0x99FF99);
        }
    }

    @Override
    public void onClose() { Minecraft.getInstance().setScreen(parent); }

    @Override public boolean isPauseScreen() { return false; }
}
