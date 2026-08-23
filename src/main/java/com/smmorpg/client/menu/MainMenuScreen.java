package com.smmorpg.client.menu;

import com.smmorpg.SmmoRPG;
import com.smmorpg.account.PlayerAccount;
import com.smmorpg.client.screen.ModServerListScreen;
import com.smmorpg.client.screen.TrainingScreen;
import com.smmorpg.rank.Rank;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * The mod's own front page, in place of the vanilla title screen.
 *
 * <p>Laid out to the sketch: settings in the top-left corner, the menu stack down the left
 * with Play and Training under it, the logo across the top, the character in the middle,
 * the inventory panel down the right, and the way out in the top-right.
 *
 * <p>Everything is positioned as a fraction of the window rather than in fixed pixels. A
 * menu that only looks right at one resolution is a menu that looks wrong for most people.
 */
public class MainMenuScreen extends Screen {

    private static final ResourceLocation LOGO = SmmoRPG.id("textures/gui/logo.png");
    private static final ResourceLocation PANEL_BG = SmmoRPG.id("textures/gui/panel.png");

    /** Colours, kept together so the whole menu can be re-skinned in one place. */
    private static final int PANEL = 0xE0121218;
    private static final int PANEL_EDGE = 0xFF2A2A36;
    private static final int ACCENT = 0xFFE03A3A;
    private static final int TEXT_DIM = 0xFF8891A0;

    private final MenuInventoryPanel inventory = new MenuInventoryPanel();

    /** Panel rectangles, recomputed on every resize. */
    private int logoX, logoY, logoW, logoH;
    private int charX, charY, charW, charH;
    private int invX, invY, invW, invH;

    public MainMenuScreen() {
        super(Component.translatable("hub.smmorpg.title"));
    }

    @Override
    protected void init() {
        int w = this.width;
        int h = this.height;

        // --- proportional frame ---
        int margin = Math.max(12, w / 60);
        int columnW = Math.max(150, w / 5);

        logoW = Math.min(360, w / 3);
        logoH = Math.max(48, h / 12);
        logoX = (w - logoW) / 2;
        logoY = margin;

        charW = Math.max(170, w / 5);
        charH = Math.max(220, (int) (h * 0.62F));
        charX = (w - charW) / 2;
        charY = logoY + logoH + margin;

        invW = Math.max(190, (int) (w * 0.24F));
        invX = w - invW - margin;
        invY = margin + 22;
        invH = h - invY - margin;

        // --- settings, top-left ---
        addRenderableWidget(Button.builder(Component.literal("⚙"),
                        b -> this.minecraft.setScreen(new net.minecraft.client.gui.screens.options.OptionsScreen(this, this.minecraft.options)))
                .bounds(margin, margin, 22, 22)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.translatable("menu.smmorpg.settings")))
                .build());

        // --- the four menu buttons ---
        int menuX = margin;
        int menuY = margin + 42;
        int menuH = 24;
        int gap = 6;

        addMenu(menuX, menuY, columnW, menuH, "hub.smmorpg.profile",
                () -> new HubScreen(this, 0));
        addMenu(menuX, menuY + (menuH + gap), columnW, menuH, "hub.smmorpg.vault",
                () -> new HubScreen(this, 1));
        addMenu(menuX, menuY + 2 * (menuH + gap), columnW, menuH, "hub.smmorpg.market",
                () -> new HubScreen(this, 2));
        addMenu(menuX, menuY + 3 * (menuH + gap), columnW, menuH, "hub.smmorpg.ranked",
                () -> new HubScreen(this, 3));

        // --- play, bottom-left, with training beside it ---
        int playY = h - margin - 28;
        int playW = columnW - 40;

        addRenderableWidget(Button.builder(Component.translatable("menu.smmorpg.play"),
                        b -> this.minecraft.setScreen(new SelectWorldScreen(this)))
                .bounds(menuX, playY, playW, 28).build());

        addRenderableWidget(Button.builder(Component.literal("T"),
                        b -> this.minecraft.setScreen(new TrainingScreen()))
                .bounds(menuX + playW + 6, playY, 28, 28)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.translatable("training.smmorpg.title")))
                .build());

        // --- multiplayer sits under the character, where the sketch put the selector bar ---
        addRenderableWidget(Button.builder(Component.translatable("servers.smmorpg.title"),
                        b -> this.minecraft.setScreen(new ModServerListScreen(this)))
                .bounds(charX + 18, charY + charH + 8, charW - 36, 22).build());

        addRenderableWidget(Button.builder(Component.literal("◀"), b -> inventory.cycle(-1))
                .bounds(charX - 4, charY + charH + 8, 18, 22).build());
        addRenderableWidget(Button.builder(Component.literal("▶"), b -> inventory.cycle(1))
                .bounds(charX + charW - 14, charY + charH + 8, 18, 22).build());

        // --- exit, top-right ---
        addRenderableWidget(Button.builder(Component.literal("✕"), b -> confirmQuit())
                .bounds(w - margin - 22, margin, 22, 22)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.translatable("menu.smmorpg.quit")))
                .build());

        inventory.layout(invX, invY, invW, invH, this::addRenderableWidget);
    }

    private void addMenu(int x, int y, int w, int h, String key,
                         java.util.function.Supplier<Screen> screen) {
        addRenderableWidget(Button.builder(Component.translatable(key),
                b -> this.minecraft.setScreen(screen.get())).bounds(x, y, w, h).build());
    }

    private void confirmQuit() {
        this.minecraft.setScreen(new ConfirmScreen(yes -> {
            if (yes) this.minecraft.stop();
            else this.minecraft.setScreen(this);
        }, Component.translatable("menu.smmorpg.quit"),
                Component.translatable("menu.smmorpg.quit_confirm")));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        this.renderBackground(g, mouseX, mouseY, partial);
        // A wash over the background so the panels read as foreground rather than as
        // widgets floating on a busy image.
        g.fill(0, 0, this.width, this.height, 0x60000000);

        drawLogo(g);
        drawCharacterPanel(g, mouseX, mouseY);
        inventory.render(g, mouseX, mouseY);

        super.render(g, mouseX, mouseY, partial);

        // Attribution stays, small and out of the way. Stripping it entirely from something
        // that gets distributed is not a corner worth cutting.
        String credit = "Minecraft © Mojang AB";
        g.drawString(this.font, credit,
                this.width - this.font.width(credit) - 4, this.height - 10, 0x55666E7A, false);
    }

    private void drawLogo(GuiGraphics g) {
        g.fill(logoX, logoY, logoX + logoW, logoY + logoH, ACCENT);
        g.fill(logoX, logoY + logoH - 2, logoX + logoW, logoY + logoH, 0xFF7A1414);

        Component name = Component.literal("SmmoRPG");
        int scale = 2;
        g.pose().pushPose();
        g.pose().translate(logoX + logoW / 2.0F, logoY + logoH / 2.0F - 4 * scale, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        g.drawString(this.font, name, -this.font.width(name) / 2, 0, 0xFFFFFF, true);
        g.pose().popPose();
    }

    private void drawCharacterPanel(GuiGraphics g, int mouseX, int mouseY) {
        g.fill(charX, charY, charX + charW, charY + charH, 0xF0000000);
        outline(g, charX, charY, charW, charH);

        PlayerAccount account = ProfileCache.get();
        Rank rank = Rank.of(account.elo(), account.matches());

        String name = account.name().isEmpty()
                ? Minecraft.getInstance().getUser().getName() : account.name();
        g.drawCenteredString(this.font, name, charX + charW / 2, charY + 8, 0xFFFFFF);
        g.drawCenteredString(this.font, Component.translatable(rank.translationKey())
                .withStyle(rank.color()), charX + charW / 2, charY + 20, 0xFFFFFF);

        // The figure fills the panel with a margin, so it scales with the window.
        int usable = charH - 60;
        int scale = Math.max(2, usable / 32);
        SkinDoll.render(g, charX + charW / 2, charY + 34 + 32 * scale,
                scale, SkinDoll.slimModel());

        g.drawCenteredString(this.font, account.elo() + " Elo",
                charX + charW / 2, charY + charH - 14, 0xFF8891A0);
    }

    private void outline(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + 1, PANEL_EDGE);
        g.fill(x, y + h - 1, x + w, y + h, PANEL_EDGE);
        g.fill(x, y, x + 1, y + h, PANEL_EDGE);
        g.fill(x + w - 1, y, x + w, y + h, PANEL_EDGE);
    }

    static int panelColour() { return PANEL; }
    static int edgeColour() { return PANEL_EDGE; }
    static int dimText() { return TEXT_DIM; }

    @Override public boolean isPauseScreen() { return false; }

    /** Escape on the front page should do nothing; there is nowhere behind it. */
    @Override public boolean shouldCloseOnEsc() { return false; }

    @Override
    public void onClose() {
        this.minecraft.setScreen(new TitleScreen());
    }
}
