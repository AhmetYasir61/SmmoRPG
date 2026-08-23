package com.smmorpg.client.menu;

import com.smmorpg.SmmoRPG;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * The page you see before you have gone anywhere.
 *
 * <p>Opened from the title screen, so everything an account owns — rank, vault, store,
 * ladder — is somewhere you can read it without first loading a world. A chest opened by a
 * command is a thing you do while playing; this is the thing you look at before deciding
 * what to play.
 *
 * <p>Whether a server is reachable or not, the hub opens. Without one it shows the last
 * account the client was sent, marked as remembered rather than live, because a menu that
 * refuses to open when the network is down is a menu you cannot trust to be there.
 */
public class HubScreen extends Screen {

    private static final int PANEL_WIDTH = 420;
    private static final int PANEL_HEIGHT = 240;
    private static final int TAB_WIDTH = 96;

    private final Screen parent;
    private final List<HubPage> pages = List.of(
            new ProfilePage(), new VaultPage(), new MarketPage(),
            new RankedPage(), new ServersPage());

    private int active;

    public HubScreen(Screen parent) {
        this(parent, 0);
    }

    /** Opens straight onto a tab, so the front page's menu buttons land where they say. */
    public HubScreen(Screen parent, int tab) {
        super(Component.translatable("hub.smmorpg.title"));
        this.parent = parent;
        this.active = Math.max(0, Math.min(tab, 4));
    }

    public Screen parent() { return parent; }

    @Override
    protected void init() {
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        for (int i = 0; i < pages.size(); i++) {
            int index = i;
            HubPage page = pages.get(i);
            addRenderableWidget(Button.builder(
                            Component.literal(page.icon() + "  ").append(page.title()),
                            b -> { active = index; rebuildWidgets(); })
                    .bounds(panelX - TAB_WIDTH - 6, panelY + 28 + i * 24, TAB_WIDTH, 20)
                    .build());
        }

        // The content area sits inside the panel with a margin the pages can rely on.
        pages.get(active).layout(this, panelX + 12, panelY + 34,
                PANEL_WIDTH - 24, PANEL_HEIGHT - 56, this::addRenderableWidget);

        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> onClose())
                .bounds(panelX + PANEL_WIDTH / 2 - 60, panelY + PANEL_HEIGHT - 24, 120, 20)
                .build());
    }

    /** Pages call this after an action that changes what other pages show. */
    public void refresh() { rebuildWidgets(); }

    public void register(AbstractWidget widget) { addRenderableWidget(widget); }

    @Override
    public void tick() {
        pages.get(active).tick();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g, mouseX, mouseY, partial);

        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        // A solid panel rather than the vanilla dirt: the hub is the mod's own space and
        // should read as one surface instead of a widget scattered over a background.
        g.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xE8101014);
        g.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + 22, 0xFF1B1B22);
        g.fill(panelX, panelY + 22, panelX + PANEL_WIDTH, panelY + 23, 0xFF3A3A48);

        g.drawString(this.font, this.title, panelX + 10, panelY + 7, 0xFFD760, false);

        HubPage page = pages.get(active);
        g.drawString(this.font, page.title(), panelX + PANEL_WIDTH - 10
                - this.font.width(page.title()), panelY + 7, 0x9AA0B0, false);

        super.render(g, mouseX, mouseY, partial);
        page.render(g, mouseX, mouseY, partial);

        if (ProfileCache.stale()) {
            g.drawString(this.font, Component.translatable("hub.smmorpg.offline"),
                    panelX + 10, panelY + PANEL_HEIGHT - 18, 0xFF8855, false);
        }
    }

    @Override
    public void onClose() { Minecraft.getInstance().setScreen(parent); }

    @Override public boolean isPauseScreen() { return false; }

    public static ResourceLocation icon(String path) {
        return SmmoRPG.id("textures/gui/" + path + ".png");
    }
}
