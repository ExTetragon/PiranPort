package com.lex0v0.piranport.client.gui;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

// 两制造弹窗共用实时筛选摘要，长条件省略显示并可悬浮查看全文。
public final class ManufacturingFilterSummary {
    private ManufacturingFilterSummary() {}

    public static Component compose(Component scope, List<Component> conditions) {
        var content=scope.copy();
        for(var condition:conditions)content.append(" · ").append(condition);
        return Component.translatable("gui.piranport.filter.summary",content);
    }

    public static Component keyword(String value) {
        return Component.translatable("gui.piranport.filter.keyword",value);
    }

    public static void draw(GuiGraphics g, Font font, Component summary, int x, int y, int width, int color) {
        String value=summary.getString();
        if(font.width(value)>width)value=font.plainSubstrByWidth(value,width-font.width("…"))+"…";
        g.drawString(font,value,x,y,color,false);
    }
}
