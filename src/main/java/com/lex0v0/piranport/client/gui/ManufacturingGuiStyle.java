package com.lex0v0.piranport.client.gui;

import com.lex0v0.piranport.PiranPort;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

// 制造界面共用高分辨率边框底纹，动态控件保留各页面的逻辑坐标。
public final class ManufacturingGuiStyle {
    public static final int TEXT = 0xffe3eaf2, MUTED = 0xffa4bacb;
    public static final int SELECTED = 0xff29b9e5, EDGE = 0xff47758f, DIVIDER = 0xff397496;
    public static final int BUTTON_TOP = 0xff496d88, BUTTON_BOTTOM = 0xff294c67;
    public static final int DISABLED_TOP = 0xff233f55, DISABLED_BOTTOM = 0xff162e43;
    public static final int MODAL_TOP = 0xff203f57, MODAL_BOTTOM = 0xff10283c;
    private static final int OUTER_EDGE = 0xff397fa6, FRAME_WIDTH = 1, SOURCE_TRIM = 3;
    private static final int TITLE_TOP = 0xff2c83b9, TITLE_BOTTOM = 0xff165180;
    private static final int PANEL_WIDTH = 384, PANEL_HEIGHT = 252, SIDE_WIDTH = 48;
    private static final ResourceLocation BACKDROP = ResourceLocation.fromNamespaceAndPath(PiranPort.MODID, "textures/gui/manufacturing_background.png");

    private ManufacturingGuiStyle() {}

    public static void background(GuiGraphics g, int x, int y, int width, int titleWidth, int previewHeight) {
        backdrop(g, x, y, width);
        title(g, x, y, width, titleWidth);
        outline(g, x, y, width, PANEL_HEIGHT, OUTER_EDGE);

        int extra = width - 348, lower = extra / 2;
        frame(g, x + 8, y + 24, 60 + extra, previewHeight);
        for (int col = 11; col < 66 + extra; col += 8)
            g.fill(x + col, y + 27, x + col + 1, y + 24 + previewHeight - 3, 0xff1d3e53);
        for (int row = 27; row < 24 + previewHeight - 3; row += 8)
            g.fill(x + 11, y + row, x + 65 + extra, y + row + 1, 0xff1d3e53);
        frame(g, x + 76 + extra, y + 24, 128, 112);
        frame(g, x + 212 + extra, y + 24, 128, 62);
        slot(g, x + 212 + extra, y + 122);
        slot(g, x + 322 + extra, y + 122);
        g.fill(x + 8, y + 143, x + width - 8, y + 144, DIVIDER);
        g.fill(x + 138 + lower, y + 152, x + 139 + lower, y + 240, DIVIDER);
        frame(g, x + 29 + lower, y + 164, 14, 54);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 4; col++) slot(g, x + 54 + lower + col * 18, y + 164 + row * 18);
            for (int col = 0; col < 9; col++) slot(g, x + 150 + lower + col * 18, y + 164 + row * 18);
        }
        for (int col = 0; col < 9; col++) slot(g, x + 150 + lower + col * 18, y + 222);
    }

    public static void shell(GuiGraphics g, int x, int y, int width, int textWidth) {
        backdrop(g,x,y,width); title(g,x,y,width,textWidth); outline(g,x,y,width,PANEL_HEIGHT,OUTER_EDGE);
    }

    private static void title(GuiGraphics g, int x, int y, int width, int textWidth) {
        int end = Math.clamp(textWidth + 24, 60, width - 60);
        for (int row = 0; row < 18; row++) {
            int right = x + end - row / 2;
            g.fill(x + FRAME_WIDTH, y + FRAME_WIDTH + row, right, y + FRAME_WIDTH + row + 1,
                    blend(TITLE_TOP, TITLE_BOTTOM, row / 17f));
            g.fill(right, y + FRAME_WIDTH + row, right + 3, y + FRAME_WIDTH + row + 1, 0xff102f49);
        }
        g.fill(x + FRAME_WIDTH, y + 19, x + width - FRAME_WIDTH, y + 20, 0xff2888b9);
    }

    private static void backdrop(GuiGraphics g, int x, int y, int width) {
        // 裁去合成图宽窄不一的外描边，九宫格保留边角比例，结构线由代码统一绘制。
        int side = SIDE_WIDTH - SOURCE_TRIM;
        int[] sourceX = {SOURCE_TRIM, SIDE_WIDTH, PANEL_WIDTH - SIDE_WIDTH, PANEL_WIDTH - SOURCE_TRIM};
        int[] sourceY = {SOURCE_TRIM, SIDE_WIDTH, PANEL_HEIGHT - SIDE_WIDTH, PANEL_HEIGHT - SOURCE_TRIM};
        int[] targetX = {FRAME_WIDTH, FRAME_WIDTH + side, width - FRAME_WIDTH - side, width - FRAME_WIDTH};
        int[] targetY = {FRAME_WIDTH, FRAME_WIDTH + side, PANEL_HEIGHT - FRAME_WIDTH - side, PANEL_HEIGHT - FRAME_WIDTH};
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                g.blit(BACKDROP, x + targetX[col], y + targetY[row],
                        targetX[col + 1] - targetX[col], targetY[row + 1] - targetY[row],
                        sourceX[col], (float) sourceY[row], sourceX[col + 1] - sourceX[col], sourceY[row + 1] - sourceY[row],
                        PANEL_WIDTH, PANEL_HEIGHT);
            }
        }
    }

    public static void frame(GuiGraphics g, int x, int y, int w, int h) {
        g.fillGradient(x, y, x + w, y + h, 0xff0e2a3e, 0xff0b2032);
        outline(g, x, y, w, h, 0xff345f7a);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, 0xff081b2b);
        g.fill(x + w - 1, y + 1, x + w, y + h, 0xff0a1d2d);
        g.fill(x + 1, y + h - 1, x + w, y + h, 0xff0a1d2d);
    }

    public static void slot(GuiGraphics g, int x, int y) {
        g.fillGradient(x + 1, y + 1, x + 17, y + 17, 0xff193c54, 0xff123148);
        g.fill(x, y, x + 18, y + 1, 0xff081c2c);
        g.fill(x, y + 1, x + 1, y + 18, 0xff0b2234);
        g.fill(x + 1, y + 17, x + 18, y + 18, EDGE);
        g.fill(x + 17, y + 1, x + 18, y + 17, 0xff315e7c);
    }

    public static void tab(GuiGraphics g, int x, int y, int w, int h, boolean selected) {
        for (int row = 0; row < h; row++) {
            int notch = Math.max(0, 4 - row / 2);
            g.fill(x + notch, y + row, x + w, y + row + 1, OUTER_EDGE);
            if (row > 0 && row < h - 1) g.fill(x + notch + 1, y + row, x + w - 1, y + row + 1,
                    blend(selected ? TITLE_TOP : 0xff284b65, selected ? TITLE_BOTTOM : 0xff173147, row / (float) (h - 1)));
        }
        if (selected) g.fill(x + 1, y + h - 2, x + w - 1, y + h + 2, TITLE_TOP);
    }

    public static void button(GuiGraphics g, int x, int y, int w, int h, boolean active, boolean highlight) {
        g.fillGradient(x, y, x + w, y + h, active ? BUTTON_TOP : DISABLED_TOP, active ? BUTTON_BOTTOM : DISABLED_BOTTOM);
        outline(g, x, y, w, h, highlight && active ? SELECTED : EDGE);
        if (active) g.fill(x + 1, y + 1, x + w - 1, y + 2, 0xff658ba5);
    }

    public static void listRow(GuiGraphics g, int x, int y, boolean available, boolean selected) {
        g.fillGradient(x, y, x + 119, y + 23, available ? BUTTON_TOP : DISABLED_TOP, available ? BUTTON_BOTTOM : DISABLED_BOTTOM);
        if (available) { g.fill(x, y, x + 119, y + 1, 0xff6b8ca3); g.fill(x, y, x + 1, y + 23, 0xff587e98); }
        if (selected) outline(g, x, y, 119, 23, SELECTED);
    }

    public static void fuel(GuiGraphics g, int x, int y, int height) {
        if (height > 0) {
            g.fillGradient(x, y + 50 - height, x + 10, y + 50, 0xffe4b949, 0xff9a681b);
            g.fill(x, y + 50 - height, x + 10, y + 51 - height, 0xfff6da80);
            g.fill(x, y + 51 - height, x + 1, y + 50, 0xffecc763);
        }
        for (int row = 2; row < 48; row += 7) g.fill(x + 7, y + row, x + 10, y + row + 1, 0xffbbd0db);
    }

    public static void modal(GuiGraphics g, int x, int y, int w, int h) {
        g.fillGradient(x, y, x + w, y + h, MODAL_TOP, MODAL_BOTTOM);
        outline(g, x, y, w, h, EDGE);
    }

    public static void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color); g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color); g.fill(x + w - 1, y, x + w, y + h, color);
    }

    private static int blend(int first, int second, float t) {
        int r = (int) (((first >> 16) & 255) * (1 - t) + ((second >> 16) & 255) * t);
        int green = (int) (((first >> 8) & 255) * (1 - t) + ((second >> 8) & 255) * t);
        int b = (int) ((first & 255) * (1 - t) + (second & 255) * t);
        return 0xff000000 | r << 16 | green << 8 | b;
    }
}
