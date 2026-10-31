package com.lex0v0.piranport.client.gui.screens;

import com.lex0v0.piranport.client.gui.ManufacturingGuiStyle;
import com.lex0v0.piranport.client.gui.ManufacturingFilterSummary;

import com.lex0v0.piranport.blockentity.developmentfactory.FactoryFilter;
import com.lex0v0.piranport.domain.equipment.EquipmentCategory;
import java.util.*;
import java.util.function.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;

// 筛选草稿只包含十五个类别和文字，取消不会修改主窗口。
public final class FactoryFilterDialog {
    private static final int WHITE=0xffe3eaf2,MUTED=0xff9db1c1;
    public final EditBox input;
    public final Button apply;
    private final List<AbstractWidget> widgets=new ArrayList<>();
    private final Map<Button,Component> labels=new LinkedHashMap<>();
    private final EnumSet<EquipmentCategory> categories=EnumSet.noneOf(EquipmentCategory.class);
    private final Font font;
    private final ToIntFunction<FactoryFilter> count;
    private final boolean all;
    private final int x,y;
    public FactoryFilterDialog(Font font,int centerX,int centerY,FactoryFilter initial,boolean all,ToIntFunction<FactoryFilter> count,Consumer<FactoryFilter> applied,Runnable closed){
        this.font=font;this.x=centerX-150;this.y=centerY-114;this.count=count;this.all=all;categories.addAll(initial.categories());
        input=new EditBox(font,x+16,y+28,252,12,tr("search_hint"));input.setBordered(false);input.setTextColor(WHITE);input.setMaxLength(128);input.setHint(tr("search_hint"));input.setValue(initial.text());widgets.add(input);
        button(276,24,12,16,Component.literal("×"),()->input.setValue(""),()->false);
        for(var group:EquipmentCategory.Group.values()){
            int col=group.ordinal(),row=0;
            for(var category:EquipmentCategory.values())if(category.group()==group){
                var label=Component.translatable(category.translationKey());
                var button=button(12+col*94,76+row++*16,88,14,label,()->{if(!categories.remove(category))categories.add(category);if(categories.size()==EquipmentCategory.values().length)categories.clear();},()->categories.contains(category));labels.put(button,label);
            }
        }
        button(12,206,88,18,tr("reset"),()->{categories.clear();input.setValue("");},()->false);
        button(106,206,88,18,tr("return"),closed,()->false);
        apply=button(200,206,88,18,tr("apply"),()->applied.accept(value()),()->false);
    }
    private static Component tr(String key,Object...args){return Component.translatable("gui.piranport.development_factory."+key,args);}
    public FactoryFilter value(){return new FactoryFilter(input.getValue(),categories);}
    public Component summary(){
        var conditions=new ArrayList<Component>();
        for(var category:EquipmentCategory.values())if(categories.contains(category))conditions.add(Component.translatable(category.translationKey()));
        if(!input.getValue().isBlank())conditions.add(ManufacturingFilterSummary.keyword(value().text()));
        return ManufacturingFilterSummary.compose(tr(all?"scope_all":"scope_permitted"),conditions);
    }
    public List<AbstractWidget> widgets(){return List.copyOf(widgets);}
    public List<Component> tooltip(double mx,double my){if(mx>=x+12&&mx<x+288&&my>=y+45&&my<y+58)return List.of(summary());for(var e:labels.entrySet())if(e.getKey().isMouseOver(mx,my))return List.of(e.getValue());return List.of();}
    private Button button(int left,int top,int width,int height,Component text,Runnable action,BooleanSupplier checked){var button=new FilterButton(x+left,y+top,width,height,text,action,checked);widgets.add(button);return button;}
    public void render(GuiGraphics g,int mx,int my,float tick){
        ManufacturingGuiStyle.modal(g, x, y, 300, 230);
        g.drawString(font,tr("search"),x+(300-font.width(tr("search")))/2,y+10,WHITE,false);
        g.fill(x+12,y+24,x+272,y+40,0xff142b3d);outline(g,x+12,y+24,260,16,input.isFocused()?0xff67b4e7:0xff58788f);
        ManufacturingFilterSummary.draw(g,font,summary(),x+12,y+47,276,MUTED);
        String[] groups={"weapons","devices","aircraft"};for(int col=0;col<3;col++)g.drawString(font,tr(groups[col]),x+12+col*94,y+62,WHITE,false);
        var matches=tr("matches",count.applyAsInt(value()));g.drawString(font,matches,x+(300-font.width(matches))/2,y+191,MUTED,false);
        widgets.forEach(w->w.render(g,mx,my,tick));
    }
    private static void outline(GuiGraphics g,int x,int y,int w,int h,int c){g.fill(x,y,x+w,y+1,c);g.fill(x,y+h-1,x+w,y+h,c);g.fill(x,y,x+1,y+h,c);g.fill(x+w-1,y,x+w,y+h,c);}
    private final class FilterButton extends Button {
        private final BooleanSupplier checked;
        private FilterButton(int x,int y,int w,int h,Component text,Runnable pressed,BooleanSupplier checked){super(x,y,w,h,text,b->pressed.run(),DEFAULT_NARRATION);this.checked=checked;}
        @Override protected void renderWidget(GuiGraphics g,int mx,int my,float tick){
            boolean on=checked.getAsBoolean();int x=getX(),y=getY(),w=getWidth(),h=getHeight();
            ManufacturingGuiStyle.button(g,x,y,w,h,true,isHoveredOrFocused()||on);
            if(on){g.fillGradient(x+1,y+2,x+w-1,y+h-1,0xff28658a,0xff174465);outline(g,x,y,w,h,ManufacturingGuiStyle.SELECTED);}
            String text=getMessage().getString();if(font.width(text)>w-6)text=font.plainSubstrByWidth(text,w-6-font.width("…"))+"…";
            g.drawString(font,text,x+(w-font.width(text))/2,y+(h-8)/2,WHITE,false);
        }
    }
}
