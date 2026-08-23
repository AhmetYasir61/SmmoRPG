package com.smmorpg.client.menu;

import com.smmorpg.account.PlayerAccount;
import com.smmorpg.rank.Rank;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/** Who you are: rank, rating, record, currency. */
public class ProfilePage extends HubPage {

    @Override public Component title() { return Component.translatable("hub.smmorpg.profile"); }
    @Override public String icon() { return "⚔"; }

    @Override protected void build(Consumer<AbstractWidget> register) {}

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        var font = Minecraft.getInstance().font;
        PlayerAccount account = ProfileCache.get();
        Rank rank = Rank.of(account.elo(), account.matches());

        int y = top;
        String name = account.name().isEmpty()
                ? Minecraft.getInstance().getUser().getName() : account.name();
        g.drawString(font, Component.literal(name), left, y, 0xFFFFFF, false);
        y += 16;

        g.drawString(font, Component.translatable(rank.translationKey())
                .withStyle(rank.color()), left, y, 0xFFFFFF, false);
        g.drawString(font, account.elo() + " Elo", left + 120, y, 0xAAB0C0, false);
        y += 14;

        // The bar toward the next rank, because a number alone does not show how close
        // you are to the thing you actually want.
        int toNext = rank.toNext(account.elo());
        if (toNext > 0) {
            int span = 250;
            float progress = Math.max(0.0F, Math.min(1.0F, 1.0F - (float) toNext / span));
            g.fill(left, y, left + 200, y + 4, 0xFF202028);
            g.fill(left, y, left + (int) (200 * progress), y + 4, rank.color().getColor() == null
                    ? 0xFF7FC4FF : 0xFF000000 | rank.color().getColor());
            g.drawString(font, Component.translatable("match.smmorpg.to_next", toNext),
                    left, y + 8, 0x777F8C, false);
            y += 22;
        } else {
            y += 8;
        }

        y += 8;
        row(g, font, left, y, Component.translatable("hub.smmorpg.record"),
                account.wins() + " / " + account.losses()); y += 14;
        row(g, font, left, y, Component.translatable("hub.smmorpg.winrate"),
                String.format("%.0f%%", account.winRate() * 100.0F)); y += 14;
        row(g, font, left, y, Component.translatable("hub.smmorpg.coins"),
                String.valueOf(account.coins())); y += 14;
        row(g, font, left, y, Component.translatable("hub.smmorpg.premium"),
                String.valueOf(account.premium())); y += 14;
        row(g, font, left, y, Component.translatable("hub.smmorpg.vault_size"),
                String.valueOf(account.vault().size()));
    }

    private static void row(GuiGraphics g, net.minecraft.client.gui.Font font,
                            int x, int y, Component label, String value) {
        g.drawString(font, label, x, y, 0x8891A0, false);
        g.drawString(font, value, x + 140, y, 0xFFFFFF, false);
    }
}
