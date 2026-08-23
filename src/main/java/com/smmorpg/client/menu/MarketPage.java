package com.smmorpg.client.menu;

import com.smmorpg.SmmoRPG;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.net.URI;
import java.util.function.Consumer;

/**
 * The store.
 *
 * <p>Real money never touches this screen. The button opens the Tebex store in the player's
 * own browser and that is the entire extent of the integration on the client — checkout,
 * card details and receipts all happen somewhere this mod cannot see. What gets bought is
 * delivered by the server the next time Tebex is polled.
 *
 * <p>Coin purchases are different: those are an account mutation and can only be made
 * against a server, so they are shown here and performed once connected.
 */
public class MarketPage extends HubPage {

    /** Filled by the server when connected; empty at the title screen. */
    private static String storeUrl = "";

    public static void setStoreUrl(String url) { storeUrl = url == null ? "" : url; }

    @Override public Component title() { return Component.translatable("hub.smmorpg.market"); }
    @Override public String icon() { return "◆"; }

    @Override
    protected void build(Consumer<AbstractWidget> register) {
        register.accept(Button.builder(Component.translatable("hub.smmorpg.open_store"),
                        b -> openStore())
                .bounds(left, top + height - 46, 160, 20).build());
    }

    private void openStore() {
        if (storeUrl.isBlank()) return;
        try {
            Util.getPlatform().openUri(URI.create(storeUrl));
        } catch (Exception e) {
            SmmoRPG.LOGGER.warn("Could not open the store URL", e);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        var font = Minecraft.getInstance().font;
        var account = ProfileCache.get();

        int y = top;
        g.drawString(font, Component.translatable("hub.smmorpg.balance"), left, y, 0x8891A0, false);
        g.drawString(font, account.coins() + " ⛁", left + 140, y, 0xFFD760, false);
        y += 14;
        g.drawString(font, Component.translatable("hub.smmorpg.premium"), left, y, 0x8891A0, false);
        g.drawString(font, String.valueOf(account.premium()), left + 140, y, 0xC08CFF, false);
        y += 24;

        g.drawString(font, Component.translatable("hub.smmorpg.market_note"),
                left, y, 0x777F8C, false);
        y += 12;
        g.drawString(font, Component.translatable("hub.smmorpg.market_note_2"),
                left, y, 0x777F8C, false);

        if (storeUrl.isBlank()) {
            g.drawString(font, Component.translatable("hub.smmorpg.store_offline"),
                    left, top + height - 24, 0xFF8855, false);
        }
    }
}
