package com.lex0v0.piranport.client.shipyard;

import com.lex0v0.piranport.blockentity.shipyard.ShipyardFilter;
import com.lex0v0.piranport.domain.ship.ShipNationality;
import com.lex0v0.piranport.domain.ship.ShipSize;
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
    private final EnumSet<ShipSize> sizes = EnumSet.noneOf(ShipSize.class);
    private final EnumSet<ShipNationality> nationalities = EnumSet.noneOf(ShipNationality.class);
    private final Font font;
    private final ToIntFunction<ShipyardFilter> count;
    private final boolean all;
    private final int x, y;

    public ShipyardFilterDialog(Font font, int centerX, int centerY, ShipyardFilter initial, boolean all,
                               ToIntFunction<ShipyardFilter> count, Consumer<ShipyardFilter> applied, Runnable closed) {
        this.font = font; this.x = centerX - 114; this.y = centerY - 103; this.count = count; this.all = all;
        sizes.addAll(initial.sizes()); nationalities.addAll(initial.nationalities());
        input = new EditBox(font, x + 16, y + 28, 176, 12, tr("search_hint"));
        input.setBordered(false); input.setTextColor(WHITE); input.setMaxLength(ShipyardFilter.MAX_TEXT_LENGTH);
        input.setHint(tr("search_hint")); input.setValue(initial.text()); widgets.add(input);
        button(200, 24, 16, 16, Component.literal("×"), () -> input.setValue(""), () -> false);
        button(12, 73, 48, 14, tr("any"), sizes::clear, sizes::isEmpty);
        for (ShipSize size : ShipSize.values()) {
            button(64 + size.ordinal() * 52, 73, 48, 14, Component.translatable(size.translationKey()), () -> {
                if (!sizes.remove(size)) sizes.add(size);
                if (sizes.size() == ShipSize.values().length) sizes.clear();
            }, () -> sizes.contains(size));
        }
        button(164, 92, 52, 14, tr("any"), nationalities::clear, nationalities::isEmpty);
        for (ShipNationality nationality : ShipNationality.values()) {
            int index = nationality.ordinal();
            var button = button(12 + index % 3 * 70, 108 + index / 3 * 16, 64, 14,
                    Component.translatable(nationality.translationKey()), () -> {
                        if (!nationalities.remove(nationality)) nationalities.add(nationality);
                        if (nationalities.size() == ShipNationality.values().length) nationalities.clear();
                    }, () -> nationalities.contains(nationality));
            countries.put(button, nationality);
        }
        button(12, 178, 64, 18, tr("reset"), () -> { sizes.clear(); nationalities.clear(); input.setValue(""); }, () -> false);
        button(82, 178, 64, 18, tr("return"), closed, () -> false);
        apply = button(152, 178, 64, 18, tr("apply"), () -> applied.accept(value()), () -> false);
    }

    private static Component tr(String key, Object... args) { return Component.translatable("gui.piranport.shipyard." + key, args); }
    public List<AbstractWidget> widgets() { return List.copyOf(widgets); }
    public ShipyardFilter value() { return new ShipyardFilter(input.getValue(), sizes, nationalities); }

    private Button button(int left, int top, int width, int height, Component label, Runnable pressed, BooleanSupplier checked) {
        var button = new FilterButton(x + left, y + top, width, height, label, pressed, checked);
        widgets.add(button); return button;
    }

    public void render(GuiGraphics g, int mx, int my, float tick) {
        g.fillGradient(x, y, x + 228, y + 206, 0xff30485c, 0xff1b2d3e);
        outline(g, x, y, 228, 206, 0xff6f94af);
        center(g, tr("search"), y + 10, WHITE);
        g.fill(x + 12, y + 24, x + 196, y + 40, 0xff142b3d);
        outline(g, x + 12, y + 24, 184, 16, input.isFocused() ? 0xff67b4e7 : 0xff58788f);
        g.drawString(font, tr(all ? "scope_all" : "scope_permitted"), x + 12, y + 47, MUTED, false);
        g.drawString(font, tr("sizes"), x + 12, y + 61, WHITE, false);
        g.drawString(font, tr("nationalities"), x + 12, y + 95, WHITE, false);
        center(g, tr("matches", count.applyAsInt(value())), y + 162, MUTED);
        widgets.forEach(widget -> widget.render(g, mx, my, tick));
    }

    public List<Component> tooltip(double mx, double my) {
        for (var entry : countries.entrySet()) if (entry.getKey().isMouseOver(mx, my))
            return List.of(Component.translatable(entry.getValue().countryTranslationKey()));
        return List.of();
    }

    private void center(GuiGraphics g, Component text, int top, int color) {
        g.drawString(font, text, x + (228 - font.width(text)) / 2, top, color, false);
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
            g.fillGradient(x,y,x+w,y+h,selected?0xff3c94ca:0xff5d788e,selected?0xff246b9c:0xff344e65);
            outline(g,x,y,w,h,isHoveredOrFocused()||selected?0xff83bde5:0xff58788f);
            g.drawString(font,getMessage(),x+(w-font.width(getMessage()))/2,y+(h-8)/2,WHITE,false);
        }
    }
}
