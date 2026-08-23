package com.smmorpg.client.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.smmorpg.SmmoRPG;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * A browser for servers actually running this mod.
 *
 * <p>Rather than making the player type an address, the list is fetched from a directory
 * endpoint that servers register themselves with. The endpoint is configurable, so a
 * community can point it at their own directory instead of the default.
 */
public class ModServerListScreen extends Screen {

    /** Directory endpoint. Built into the mod, so a client needs no configuration at all. */
    public static volatile String directoryUrl =
            com.smmorpg.backend.BackendEndpoints.SERVER_DIRECTORY_URL;

    private record Entry(String name, String address, int players, int maxPlayers, String version) {}

    private final Screen parent;
    private final List<Entry> entries = new ArrayList<>();
    private String status = "";

    public ModServerListScreen(Screen parent) {
        super(Component.translatable("servers.smmorpg.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("servers.smmorpg.refresh"),
                        b -> refresh())
                .bounds(this.width / 2 - 154, this.height - 28, 150, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> onClose())
                .bounds(this.width / 2 + 4, this.height - 28, 150, 20).build());

        for (int i = 0; i < entries.size() && i < 8; i++) {
            Entry entry = entries.get(i);
            addRenderableWidget(Button.builder(
                            Component.literal(entry.name() + "  (" + entry.players() + "/" + entry.maxPlayers() + ")"),
                            b -> connect(entry))
                    .bounds(this.width / 2 - 154, 44 + i * 24, 308, 20).build());
        }

        if (entries.isEmpty() && status.isEmpty()) refresh();
    }

    private void refresh() {
        if (directoryUrl.isBlank()) {
            status = Component.translatable("servers.smmorpg.no_directory").getString();
            return;
        }
        status = Component.translatable("servers.smmorpg.loading").getString();

        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build()
                .sendAsync(HttpRequest.newBuilder(URI.create(directoryUrl))
                                .timeout(Duration.ofSeconds(12)).GET().build(),
                        HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Minecraft.getInstance().execute(() -> {
                    try {
                        entries.clear();
                        // The service answers {"servers":[...]}; a bare array is accepted
                        // too so a hand-written directory file still works.
                        var root = JsonParser.parseString(response.body());
                        JsonArray array = root.isJsonArray()
                                ? root.getAsJsonArray()
                                : root.getAsJsonObject().getAsJsonArray("servers");
                        for (var element : array) {
                            JsonObject o = element.getAsJsonObject();
                            entries.add(new Entry(
                                    o.get("name").getAsString(),
                                    o.get("address").getAsString(),
                                    o.has("players") ? o.get("players").getAsInt() : 0,
                                    o.has("max_players") ? o.get("max_players").getAsInt() : 0,
                                    o.has("version") ? o.get("version").getAsString() : "?"));
                        }
                        status = entries.isEmpty()
                                ? Component.translatable("servers.smmorpg.empty").getString() : "";
                        rebuildWidgets();
                    } catch (Exception e) {
                        SmmoRPG.LOGGER.warn("Server directory parse failed", e);
                        status = Component.translatable("servers.smmorpg.error").getString();
                    }
                }))
                .exceptionally(t -> {
                    Minecraft.getInstance().execute(() ->
                            status = Component.translatable("servers.smmorpg.error").getString());
                    return null;
                });
    }

    private void connect(Entry entry) {
        ServerData data = new ServerData(entry.name(), entry.address(), ServerData.Type.OTHER);
        ConnectScreen.startConnecting(this, Minecraft.getInstance(),
                ServerAddress.parseString(entry.address()), data, false, null);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g, mouseX, mouseY, partial);
        super.render(g, mouseX, mouseY, partial);
        g.drawCenteredString(this.font, this.title, this.width / 2, 16, 0xFFFFFF);
        if (!status.isEmpty()) {
            g.drawCenteredString(this.font, status, this.width / 2, this.height / 2, 0xAAAAAA);
        }
    }

    @Override
    public void onClose() { Minecraft.getInstance().setScreen(parent); }
}
