package com.smmorpg.client.menu;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * The character in the middle of the menu.
 *
 * <p>Drawn from the player's own skin texture rather than by rendering a real entity. That
 * is not the first choice — a rotating model would be nicer — but an entity needs a level to
 * live in and at the title screen there is no level to put one in. Cutting the skin's UV
 * regions and laying them out gives a recognisably <em>your</em> character with nothing
 * behind it that can be null.
 *
 * <p>The overlay layer (hat, jacket, sleeves) is drawn over the base, so a skin whose detail
 * lives entirely in the second layer still reads correctly.
 */
public final class SkinDoll {

    /** Skin texture regions, in the 64x64 layout. */
    private record Part(int u, int v, int w, int h, int overlayU, int overlayV) {}

    private static final Part HEAD = new Part(8, 8, 8, 8, 40, 8);
    private static final Part BODY = new Part(20, 20, 8, 12, 20, 36);
    private static final Part RIGHT_ARM = new Part(44, 20, 4, 12, 44, 36);
    private static final Part LEFT_ARM = new Part(36, 52, 4, 12, 52, 52);
    private static final Part RIGHT_LEG = new Part(4, 20, 4, 12, 4, 36);
    private static final Part LEFT_LEG = new Part(20, 52, 4, 12, 4, 52);

    private SkinDoll() {}

    /**
     * Draws the doll centred on {@code centreX}, standing on {@code baseY}.
     *
     * @param scale pixels per skin pixel; the whole figure is 32 skin pixels tall
     */
    public static void render(GuiGraphics g, int centreX, int baseY, int scale, boolean slimArms) {
        ResourceLocation skin = skinTexture();
        if (skin == null) return;

        RenderSystem.enableBlend();

        int armWidth = slimArms ? 3 : 4;
        int top = baseY - 32 * scale;

        // Head sits on top, body below it, limbs either side and under. Laid out in skin
        // pixels first and multiplied up, so every seam lands on an exact boundary.
        blit(g, skin, centreX - 4 * scale, top, 8 * scale, 8 * scale, HEAD);
        blit(g, skin, centreX - 4 * scale, top + 8 * scale, 8 * scale, 12 * scale, BODY);

        blit(g, skin, centreX - (4 + armWidth) * scale, top + 8 * scale,
                armWidth * scale, 12 * scale, RIGHT_ARM);
        blit(g, skin, centreX + 4 * scale, top + 8 * scale,
                armWidth * scale, 12 * scale, LEFT_ARM);

        blit(g, skin, centreX - 4 * scale, top + 20 * scale, 4 * scale, 12 * scale, RIGHT_LEG);
        blit(g, skin, centreX, top + 20 * scale, 4 * scale, 12 * scale, LEFT_LEG);

        RenderSystem.disableBlend();
    }

    private static void blit(GuiGraphics g, ResourceLocation skin, int x, int y,
                             int w, int h, Part part) {
        g.blit(skin, x, y, w, h, part.u(), part.v(), part.w(), part.h(), 64, 64);
        // Second layer on top. A skin that keeps its hair or armour here would look bald
        // and unarmoured without this pass.
        g.blit(skin, x, y, w, h, part.overlayU(), part.overlayV(), part.w(), part.h(), 64, 64);
    }

    /** The player's own skin if the game has it yet, otherwise the default. */
    public static ResourceLocation skinTexture() {
        Minecraft mc = Minecraft.getInstance();
        try {
            var profile = mc.getGameProfile();
            if (profile != null) {
                return mc.getSkinManager().getInsecureSkin(profile).texture();
            }
        } catch (Exception ignored) {
            // No session, offline mode, a skin server having a bad day — all fine.
        }
        return net.minecraft.client.resources.DefaultPlayerSkin.getDefaultTexture();
    }

    public static boolean slimModel() {
        Minecraft mc = Minecraft.getInstance();
        try {
            var profile = mc.getGameProfile();
            if (profile != null) {
                return mc.getSkinManager().getInsecureSkin(profile).model()
                        == net.minecraft.client.resources.PlayerSkin.Model.SLIM;
            }
        } catch (Exception ignored) {
            // Fall through to the wide arms, which is the safer default of the two.
        }
        return false;
    }
}
