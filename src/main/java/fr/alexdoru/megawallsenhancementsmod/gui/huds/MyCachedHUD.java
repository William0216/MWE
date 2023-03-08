package fr.alexdoru.megawallsenhancementsmod.gui.huds;

import fr.alexdoru.megawallsenhancementsmod.gui.guiapi.GuiPosition;
import fr.alexdoru.megawallsenhancementsmod.gui.guiapi.ICachedHUDText;
import fr.alexdoru.megawallsenhancementsmod.gui.guiapi.IRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;

import java.util.List;

public abstract class MyCachedHUD extends Gui implements IRenderer, ICachedHUDText {

    public static final Minecraft mc = Minecraft.getMinecraft();
    public static final FontRenderer frObj = mc.fontRendererObj;

    public String displayText = "";
    public final GuiPosition guiPosition;

    public MyCachedHUD(GuiPosition guiPosition) {
        this.guiPosition = guiPosition;
    }

    @Override
    public void updateDisplayText() {}

    @Override
    public int getHeight() {
        return mc.fontRendererObj.FONT_HEIGHT;
    }

    @Override
    public int getWidth() {
        return mc.fontRendererObj.getStringWidth(displayText);
    }

    @Override
    public void render(ScaledResolution resolution) {}

    @Override
    public void renderDummy() {}

    @Override
    public boolean isEnabled(long currentTimeMillis) {
        return true;
    }

    @Override
    public GuiPosition getHUDPosition() {
        return this.guiPosition;
    }

    protected void drawStringList(List<String> list, int x, int y, boolean dropShadow, boolean centered) {
        for (final String line : list) {
            mc.fontRendererObj.drawString(line, (float) x, centered ? (float) (x - mc.fontRendererObj.getStringWidth(line) / 2) : (float) y, 0xFFFFFF, dropShadow);
            y += mc.fontRendererObj.FONT_HEIGHT;
        }
    }

    protected int getMultilineWidth(List<String> list) {
        int maxwidth = 0;
        for (final String line : list) {
            final int width = mc.fontRendererObj.getStringWidth(line);
            if (width > maxwidth) {
                maxwidth = width;
            }
        }
        return maxwidth;
    }

}
