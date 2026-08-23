package com.smmorpg.client.menu;

import com.smmorpg.arena.MatchMode;
import com.smmorpg.rank.LeaderboardEntry;
import com.smmorpg.rank.Rank;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * Queues and the ladder.
 *
 * <p>The queue buttons only do anything once you are on a server, because a queue is a
 * server-side thing by definition — you cannot be matched against someone from a menu that
 * is not connected to anything. Pressed at the title screen they say so rather than
 * pretending to work.
 */
public class RankedPage extends HubPage {

    /** Pushed down by the server; empty until then. */
    private static List<LeaderboardEntry> ladder = List.of();

    public static void setLadder(List<LeaderboardEntry> entries) {
        ladder = entries == null ? List.of() : List.copyOf(entries);
    }

    @Override public Component title() { return Component.translatable("hub.smmorpg.ranked"); }
    @Override public String icon() { return "★"; }

    @Override
    protected void build(Consumer<AbstractWidget> register) {
        int x = left;
        for (MatchMode mode : MatchMode.values()) {
            register.accept(Button.builder(Component.translatable(mode.translationKey()),
                            b -> queue(mode))
                    .bounds(x, top, 110, 20).build());
            x += 116;
        }
    }

    private void queue(MatchMode mode) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return;      // handled visually below
        mc.player.connection.sendCommand("smmorpg queue " + mode.key());
        mc.setScreen(null);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        var font = Minecraft.getInstance().font;
        boolean connected = Minecraft.getInstance().getConnection() != null;

        if (!connected) {
            g.drawString(font, Component.translatable("hub.smmorpg.queue_needs_server"),
                    left, top + 26, 0xFF8855, false);
        }

        int y = top + 42;
        g.drawString(font, Component.translatable("hub.smmorpg.ladder"), left, y, 0xFFD760, false);
        y += 14;

        if (ladder.isEmpty()) {
            g.drawString(font, Component.translatable("hub.smmorpg.ladder_empty"),
                    left, y, 0x777F8C, false);
            return;
        }

        int shown = Math.min(ladder.size(), Math.max(1, (top + height - y - 4) / 12));
        for (int i = 0; i < shown; i++) {
            LeaderboardEntry entry = ladder.get(i);
            Rank rank = entry.rank();

            g.drawString(font, "#" + entry.position(), left, y, 0x777F8C, false);
            g.drawString(font, entry.name(), left + 34, y, 0xFFFFFF, false);
            g.drawString(font, Component.translatable(rank.translationKey())
                    .withStyle(rank.color()), left + 150, y, 0xFFFFFF, false);
            g.drawString(font, String.valueOf(entry.elo()), left + 250, y, 0xAAB0C0, false);
            g.drawString(font, entry.wins() + "-" + entry.losses(), left + 300, y, 0x777F8C, false);
            y += 12;
        }
    }
}
