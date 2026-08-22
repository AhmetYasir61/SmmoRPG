package com.smmorpg.client.screen;

import com.smmorpg.client.ClientState;
import com.smmorpg.network.C2SLearnSkill;
import com.smmorpg.skill.Skill;
import com.smmorpg.skill.SkillData;
import com.smmorpg.skill.Skills;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import com.smmorpg.client.ClientNet;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** The skill tree, laid out by category. */
public class SkillScreen extends Screen {

    private static final String[] CATEGORIES = {
            Skill.CATEGORY_OFFENSE, Skill.CATEGORY_DEFENSE,
            Skill.CATEGORY_MOVEMENT, Skill.CATEGORY_SURVIVAL};

    public SkillScreen() {
        super(Component.translatable("screen.smmorpg.skills"));
    }

    @Override
    protected void init() {
        int columnWidth = Math.min(140, (this.width - 40) / CATEGORIES.length);
        int startX = (this.width - columnWidth * CATEGORIES.length) / 2;

        for (int c = 0; c < CATEGORIES.length; c++) {
            List<Skill> inCategory = new ArrayList<>();
            for (Skill s : Skills.all().values()) {
                if (s.category().equals(CATEGORIES[c])) inCategory.add(s);
            }
            for (int i = 0; i < inCategory.size(); i++) {
                Skill skill = inCategory.get(i);
                addRenderableWidget(Button.builder(
                                Component.translatable(skill.translationKey()),
                                b -> ClientNet.sendToServer(new C2SLearnSkill(skill.id().toString())))
                        .bounds(startX + c * columnWidth, 60 + i * 22, columnWidth - 6, 20).build());
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g, mouseX, mouseY, partial);
        super.render(g, mouseX, mouseY, partial);

        SkillData data = ClientState.skills;
        g.drawCenteredString(this.font, this.title, this.width / 2, 16, 0xFFFFFF);
        g.drawCenteredString(this.font,
                Component.translatable("screen.smmorpg.skill_points", data.skillPoints()),
                this.width / 2, 30, data.skillPoints() > 0 ? 0xFFFF55 : 0x888888);

        int columnWidth = Math.min(140, (this.width - 40) / CATEGORIES.length);
        int startX = (this.width - columnWidth * CATEGORIES.length) / 2;
        for (int c = 0; c < CATEGORIES.length; c++) {
            g.drawString(this.font, Component.translatable("skill.smmorpg.category." + CATEGORIES[c]),
                    startX + c * columnWidth, 48, 0xFFD760);

            int i = 0;
            for (Skill s : Skills.all().values()) {
                if (!s.category().equals(CATEGORIES[c])) continue;
                // The rank sits to the right of each button rather than inside its label,
                // so a long skill name never pushes the number off the widget.
                g.drawString(this.font, data.rank(s) + "/" + s.maxRank(),
                        startX + c * columnWidth + columnWidth - 26, 66 + i * 22, 0xCCCCCC);
                i++;
            }
        }
    }
}
