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

/**
 * Two buttons, one decision, and the player makes it: update and restart now, or wait.
 *
 * <p>"Update and restart" is a single action — download, verify, relaunch — so the player
 * never has to come back and finish the job. "Wait" closes the screen and changes nothing;
 * the update stays pending and can be applied from the pause menu whenever they are ready.
 */
public class UpdateScreen extends Screen {

    private final UpdateManifest manifest;
    private final Screen parent;

    private String status = "";
    private boolean working = false;
    /** Set when the jar is on disk but the platform would not let us relaunch. */
    private boolean needsManualRestart = false;

    public UpdateScreen(UpdateManifest manifest, Screen parent) {
        super(Component.translatable("update.smmorpg.title"));
        this.manifest = manifest;
        this.parent = parent;
    }

    @Override
    protected void init() {
        int y = this.height / 2 + 26;

        if (needsManualRestart) {
            // The download succeeded; only the automatic relaunch did not. Quitting is
            // still enough — the staged jar loads on the next start either way.
            addRenderableWidget(Button.builder(Component.translatable("update.smmorpg.quit_to_apply"),
                            b -> Minecraft.getInstance().stop())
                    .bounds(this.width / 2 - 100, y, 200, 20).build());
            return;
        }

        addRenderableWidget(Button.builder(
                        Component.translatable("update.smmorpg.update_and_restart"),
                        b -> updateAndRestart())
                .bounds(this.width / 2 - 154, y, 150, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("update.smmorpg.wait"),
                        b -> onClose())
                .bounds(this.width / 2 + 4, y, 150, 20).build());
    }

    private void updateAndRestart() {
        if (working) return;
        working = true;
        status = Component.translatable("update.smmorpg.downloading").getString();

        UpdateService.download(FMLPaths.MODSDIR.get()).thenAccept(ok ->
                Minecraft.getInstance().execute(() -> {
                    working = false;
                    if (!ok) {
                        status = Component.translatable("update.smmorpg.failed").getString();
                        return;
                    }
                    status = Component.translatable("update.smmorpg.restarting").getString();

                    if (UpdateService.relaunch()) {
                        Minecraft.getInstance().stop();
                    } else {
                        needsManualRestart = true;
                        status = Component.translatable("update.smmorpg.manual_restart").getString();
                        rebuildWidgets();
                    }
                }));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g, mouseX, mouseY, partial);
        super.render(g, mouseX, mouseY, partial);

        int cx = this.width / 2;
        g.drawCenteredString(this.font, this.title, cx, this.height / 2 - 60, 0xFFFFFF);
        g.drawCenteredString(this.font, Component.translatable("update.smmorpg.version_line",
                        com.smmorpg.update.UpdateClient.currentVersion(), manifest.version()),
                cx, this.height / 2 - 42, 0xFFD760);

        if (!manifest.notes().isBlank()) {
            g.drawCenteredString(this.font, manifest.notes(), cx, this.height / 2 - 26, 0xAAAAAA);
        }
        if (manifest.mandatory()) {
            g.drawCenteredString(this.font,
                    Component.translatable("update.smmorpg.mandatory").withStyle(ChatFormatting.RED),
                    cx, this.height / 2 - 10, 0xFF5555);
        }
        if (!status.isEmpty()) {
            g.drawCenteredString(this.font, status, cx, this.height / 2 + 8,
                    needsManualRestart ? 0xFFD760 : 0x99FF99);
        }
    }

    @Override
    public void onClose() { Minecraft.getInstance().setScreen(parent); }

    @Override public boolean isPauseScreen() { return false; }
}
