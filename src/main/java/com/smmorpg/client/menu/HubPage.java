package com.smmorpg.client.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * One tab of the hub.
 *
 * <p>A page owns its widgets and its drawing and knows nothing about the others, so adding
 * a tab is adding a class rather than editing a switch in four places.
 */
public abstract class HubPage {

    protected final List<AbstractWidget> widgets = new ArrayList<>();
    protected HubScreen hub;
    protected int left, top, width, height;

    public abstract Component title();

    /** Icon shown on the tab strip. A single character keeps the strip narrow. */
    public abstract String icon();

    /** Called whenever the page is opened or the window is resized. */
    public void layout(HubScreen hub, int left, int top, int width, int height,
                       Consumer<AbstractWidget> register) {
        this.hub = hub;
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        widgets.clear();
        build(register);
    }

    protected abstract void build(Consumer<AbstractWidget> register);

    public abstract void render(GuiGraphics g, int mouseX, int mouseY, float partial);

    /** Pages that need to poll something override this. */
    public void tick() {}

    protected int centreX() { return left + width / 2; }
}
