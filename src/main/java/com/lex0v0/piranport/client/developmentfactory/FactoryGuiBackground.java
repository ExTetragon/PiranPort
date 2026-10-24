package com.lex0v0.piranport.client.developmentfactory;

import com.lex0v0.piranport.PiranPort;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

// 沿用船坞的底纹与槽位贴图，按工厂尺寸重排，避免拉伸网格和金属边缘。
public final class FactoryGuiBackground {
    private static final ResourceLocation SHIPYARD = ResourceLocation.fromNamespaceAndPath(PiranPort.MODID, "textures/gui/shipyard.png");
    private FactoryGuiBackground() {}

    public static void draw(GuiGraphics g, int x, int y, int titleWidth) {
        // 取样条两侧保留净底纹，避免非整数GUI缩放采到邻接边框。
        for (int offset = 2; offset < 382; offset += 5) {
            int width = Math.min(5, 382 - offset);
            blit(g, x + offset, y + 20, 69, 20, width, 124);
            blit(g, x + offset, y + 144, 14, 144, width, 108);
        }
        g.fill(x + 2, y + 143, x + 382, y + 144, 0xff24313c);
        blit(g, x, y, 0, 0, 2, 252);
        blit(g, x + 382, y, 346, 0, 2, 252);
        g.fill(x + 2, y, x + 382, y + 2, 0xff647a8b);
        g.fillGradient(x + 2, y + 2, x + 382, y + 20, 0xff243d4f, 0xff162837);
        int end = Math.clamp(titleWidth + 24, 75, 324);
        for (int row = 0; row < 17; row++) {
            float t = row / 18f;
            int r = (int) (44 - 21 * t), green = (int) (157 - 54 * t), b = (int) (233 - 61 * t);
            g.fill(x + 2, y + 2 + row, x + end - row / 2, y + 3 + row, 0xff000000 | r << 16 | green << 8 | b);
        }
        g.fill(x + 2, y + 19, x + 382, y + 20, 0xff328fce);
        blit(g, x + 351, y + 4, 315, 4, 18, 12);

        frame(g, x + 8, y + 24, 96, 48);
        for (int col = 11; col < 102; col += 8) g.fill(x + col, y + 27, x + col + 1, y + 69, 0xff243c4c);
        for (int row = 27; row < 70; row += 8) g.fill(x + 11, y + row, x + 101, y + row + 1, 0xff243c4c);
        frame(g, x + 112, y + 24, 128, 112);
        frame(g, x + 248, y + 24, 128, 62);
        slot(g, x + 248, y + 122);
        slot(g, x + 358, y + 122);

        g.fill(x + 8, y + 143, x + 376, y + 144, 0xff58748a);
        g.fill(x + 156, y + 152, x + 157, y + 240, 0xff58748a);
        blit(g, x + 47, y + 164, 29, 164, 14, 54);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 4; col++) slot(g, x + 72 + col * 18, y + 164 + row * 18);
            for (int col = 0; col < 9; col++) slot(g, x + 168 + col * 18, y + 164 + row * 18);
        }
        for (int col = 0; col < 9; col++) slot(g, x + 168 + col * 18, y + 222);
    }

    private static void slot(GuiGraphics g, int x, int y) { blit(g, x, y, 54, 164, 18, 18); }
    private static void blit(GuiGraphics g, int x, int y, int u, int v, int width, int height) {
        g.blit(SHIPYARD, x, y, u, v, width, height, 348, 252);
    }
    private static void frame(GuiGraphics g, int x, int y, int width, int height) {
        g.fill(x, y, x + width, y + height, 0xff506a7e);
        g.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xff172634);
        g.fill(x + width - 1, y + 1, x + width, y + height, 0xff10212e);
        g.fill(x + 1, y + height - 1, x + width, y + height, 0xff10212e);
    }
}
