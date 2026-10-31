package com.lex0v0.piranport.client.gui.screens;

import com.lex0v0.piranport.client.gui.ManufacturingGuiStyle;
import com.lex0v0.piranport.client.gui.ManufacturingFilterSummary;

import com.lex0v0.piranport.blockentity.shipyard.ShipyardFilter;
import com.lex0v0.piranport.domain.ship.ShipNationality;
import com.lex0v0.piranport.domain.ship.ShipSize;
import com.lex0v0.piranport.domain.ship.ShipType;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

// 筛选弹窗只编辑临时条件，应用后交给主界面同步与保存。
public final class ShipyardFilterDialog {
    private static final int WHITE = 0xffe3eaf2, MUTED = 0xff9db1c1;
    public final EditBox input;
    public final Button apply;
    private final List<AbstractWidget> widgets = new ArrayList<>();
    private final Map<Button, ShipNationality> countries = new LinkedHashMap<>();
    private final Map<Button, Component> typeLabels = new LinkedHashMap<>();
    private final EnumSet<ShipNationality> nationalities = EnumSet.noneOf(ShipNationality.class);
    private final EnumSet<ShipType> types = EnumSet.noneOf(ShipType.class);
    private final Font font;
    private final ToIntFunction<ShipyardFilter> count;
    private final boolean all;
    private final int x, y;

    public ShipyardFilterDialog(Font font, int centerX, int centerY, ShipyardFilter initial, boolean all,
                               ToIntFunction<ShipyardFilter> count, Consumer<ShipyardFilter> applied, Runnable closed) {
        this.font = font; this.x = centerX - 174; this.y = centerY - 105; this.count = count; this.all = all;
        nationalities.addAll(initial.nationalities());
        if(!initial.sizes().isEmpty()||!initial.types().isEmpty())for(var type:ShipType.values()) {
            if((initial.sizes().isEmpty()||initial.sizes().contains(type.size()))&&(initial.types().isEmpty()||initial.types().contains(type)))types.add(type);
        }
        input = new EditBox(font, x + 16, y + 28, 292, 12, tr("search_hint"));
        input.setBordered(false); input.setTextColor(WHITE); input.setMaxLength(ShipyardFilter.MAX_TEXT_LENGTH);
        input.setHint(tr("search_hint")); input.setValue(initial.text()); widgets.add(input);
        button(320, 24, 16, 16, Component.literal("×"), () -> input.setValue(""), () -> false);
        button(12, 73, 48, 14, tr("any"), types::clear, this::unrestricted);
        for (ShipSize size : ShipSize.values()) {
            button(64 + size.ordinal() * 52, 73, 48, 14, Component.translatable(size.translationKey()), () -> {
                if(sizeSelected(size))types.removeIf(type->type.size()==size);
                else for(var type:ShipType.values())if(type.size()==size)types.add(type);
            }, () -> sizeSelected(size));
        }
        button(284, 92, 52, 14, tr("any"), types::clear, this::unrestricted);
        for (ShipType type : ShipType.values()) {
            int index=type.ordinal();
            Component label=tr("type."+type.path());
            var button=button(12+index*36,108,34,14,label,()->{
                if(!types.remove(type))types.add(type);
            },()->types.contains(type));
            typeLabels.put(button,Component.translatable(type.translationKey()));
        }
        button(284, 128, 52, 14, tr("any"), nationalities::clear, nationalities::isEmpty);
        for (ShipNationality nationality : ShipNationality.values()) {
            int index = nationality.ordinal();
            var button = button(12 + index * 36, 144, 34, 14,
                    tr("nation."+nationality.path()), () -> {
                        if (!nationalities.remove(nationality)) nationalities.add(nationality);
                        if (nationalities.size() == ShipNationality.values().length) nationalities.clear();
                    }, () -> nationalities.contains(nationality));
            countries.put(button, nationality);
        }
        button(12, 184, 104, 18, tr("reset"), () -> { nationalities.clear(); types.clear(); input.setValue(""); }, () -> false);
        button(122, 184, 104, 18, tr("return"), closed, () -> false);
        apply = button(232, 184, 104, 18, tr("apply"), () -> applied.accept(value()), () -> false);
    }

    private static Component tr(String key, Object... args) { return Component.translatable("gui.piranport.shipyard." + key, args); }
    public List<AbstractWidget> widgets() { return List.copyOf(widgets); }
    // 舰种是联动后的实际条件，船型只表示完整分组，避免再次取交集丢失其他组的部分选择。
    public ShipyardFilter value() { return new ShipyardFilter(input.getValue(), java.util.Set.of(), nationalities, types); }
    private boolean unrestricted(){return types.isEmpty()||types.size()==ShipType.values().length;}
    private boolean sizeSelected(ShipSize size){
        for(var type:ShipType.values())if(type.size()==size&&!types.contains(type))return false;
        return true;
    }

    public Component summary() {
        var conditions=new ArrayList<Component>();
        if(!unrestricted()){
            for(var size:ShipSize.values())if(sizeSelected(size))conditions.add(Component.translatable(size.translationKey()));
            for(var type:ShipType.values())if(types.contains(type))conditions.add(Component.translatable(type.translationKey()));
        }
        for(var nation:ShipNationality.values())if(nationalities.contains(nation))conditions.add(Component.translatable(nation.translationKey()));
        if(!input.getValue().isBlank())conditions.add(ManufacturingFilterSummary.keyword(value().text()));
        return ManufacturingFilterSummary.compose(tr(all?"scope_all":"scope_permitted"),conditions);
    }

    private Button button(int left, int top, int width, int height, Component label, Runnable pressed, BooleanSupplier checked) {
        var button = new FilterButton(x + left, y + top, width, height, label, pressed, checked);
        widgets.add(button); return button;
    }

    public void render(GuiGraphics g, int mx, int my, float tick) {
        ManufacturingGuiStyle.modal(g, x, y, 348, 210);
        center(g, tr("search"), y + 10, WHITE);
        g.fill(x + 12, y + 24, x + 316, y + 40, 0xff142b3d);
        outline(g, x + 12, y + 24, 304, 16, input.isFocused() ? 0xff67b4e7 : 0xff58788f);
        ManufacturingFilterSummary.draw(g,font,summary(),x+12,y+47,324,MUTED);
        g.drawString(font, tr("sizes"), x + 12, y + 61, WHITE, false);
        g.drawString(font, tr("types"), x + 12, y + 95, WHITE, false);
        g.drawString(font, tr("nationalities"), x + 12, y + 131, WHITE, false);
        center(g, tr("matches", count.applyAsInt(value())), y + 168, MUTED);
        widgets.forEach(widget -> widget.render(g, mx, my, tick));
    }

    public List<Component> tooltip(double mx, double my) {
        if(mx>=x+12&&mx<x+336&&my>=y+45&&my<y+58)return List.of(summary());
        for(var entry:typeLabels.entrySet())if(entry.getKey().isMouseOver(mx,my))return List.of(entry.getValue());
        for (var entry : countries.entrySet()) if (entry.getKey().isMouseOver(mx, my))
            return List.of(Component.translatable(entry.getValue().countryTranslationKey()));
        return List.of();
    }

    private void center(GuiGraphics g, Component text, int top, int color) {
        g.drawString(font, text, x + (348 - font.width(text)) / 2, top, color, false);
    }

    private static void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x,y,x+w,y+1,color); g.fill(x,y+h-1,x+w,y+h,color);
        g.fill(x,y,x+1,y+h,color); g.fill(x+w-1,y,x+w,y+h,color);
    }

    private final class FilterButton extends Button {
        private final BooleanSupplier checked;
        private FilterButton(int x, int y, int w, int h, Component label, Runnable pressed, BooleanSupplier checked) {
            super(x,y,w,h,label,b -> pressed.run(),DEFAULT_NARRATION); this.checked = checked;
        }
        @Override protected void renderWidget(GuiGraphics g, int mx, int my, float tick) {
            int x=getX(), y=getY(), w=getWidth(), h=getHeight(); boolean selected=checked.getAsBoolean();
            ManufacturingGuiStyle.button(g, x, y, w, h, true, isHoveredOrFocused() || selected);
            if (selected) { g.fillGradient(x+1,y+2,x+w-1,y+h-1,0xff28658a,0xff174465); outline(g,x,y,w,h,ManufacturingGuiStyle.SELECTED); }
            String label=getMessage().getString();if(font.width(label)>w-4)label=font.plainSubstrByWidth(label,w-4-font.width("…"))+"…";
            g.drawString(font,label,x+(w-font.width(label))/2,y+(h-8)/2,WHITE,false);
        }
    }
}
