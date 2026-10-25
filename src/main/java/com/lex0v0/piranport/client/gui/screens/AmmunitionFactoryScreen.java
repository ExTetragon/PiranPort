package com.lex0v0.piranport.client.gui.screens;

import com.lex0v0.piranport.blockentity.ammunitionfactory.AmmunitionJob;
import com.lex0v0.piranport.blockentity.manufacturing.AmmunitionMaterials;
import com.lex0v0.piranport.client.gui.ManufacturingGuiStyle;
import com.lex0v0.piranport.domain.ammunition.*;
import com.lex0v0.piranport.menu.AmmunitionFactoryMenu;
import com.lex0v0.piranport.network.payload.AmmunitionFactoryActionPayload;
import com.lex0v0.piranport.recipe.AmmunitionFactoryRecipe;
import com.lex0v0.piranport.registry.ModItems;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

// 共用原有背景与控件风格，浏览需求和运行进度分别显示。
public final class AmmunitionFactoryScreen extends AbstractContainerScreen<AmmunitionFactoryMenu> {
    private static final int TEXT=ManufacturingGuiStyle.TEXT,MUTED=ManufacturingGuiStyle.MUTED,ORANGE=0xffedb05d;
    private static final int DEMAND_SWITCH_X=220,DEMAND_SWITCH_WIDTH=18,DEMAND_SWITCH_THUMB=7;
    private final List<LineButton> categories=new ArrayList<>(),types=new ArrayList<>();
    private LineButton run;
    private EditBox quantity;
    private float scale=1;
    private boolean dragging,dirty,settingText;
    public AmmunitionFactoryScreen(AmmunitionFactoryMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=348;imageHeight=252;}
    private static Component tr(String key,Object...args){return Component.translatable("gui.piranport.ammunition_factory."+key,args);}
    private AmmunitionFactoryRecipe recipe(){return menu.factory.recipe(menu.browse.selected.id());}
    private AmmunitionJob job(){return menu.factory.job();}
    private List<AmmunitionType> entries(){return Arrays.stream(AmmunitionType.values()).filter(t->t.category()==menu.browse.category()).toList();}
    private void action(int kind,CompoundTag data){PacketDistributor.sendToServer(new AmmunitionFactoryActionPayload(menu.containerId,kind,data));}
    private void browseChanged(boolean commit){var t=menu.browse.save();t.putBoolean("CommitQuantity",commit);action(0,t);}
    private long input(){try{return Long.parseLong(quantity.getValue());}catch(NumberFormatException e){return 0;}}
    private int normalized(){var r=recipe();return r==null?0:AmmunitionMaterials.normalize(input(),r.count(),menu.factory.maximum(r));}
    private void displayQuantity(int value){settingText=true;quantity.setValue(Integer.toString(value));settingText=false;dirty=false;}
    private void commitQuantity(){if(quantity==null)return;menu.browse.quantity=normalized();displayQuantity(menu.browse.quantity);browseChanged(true);}
    private void select(AmmunitionType type){commitQuantity();menu.browse.selected=type;menu.normalizeQuantity();displayQuantity(menu.browse.quantity);browseChanged(true);refresh();}
    @Override protected void init(){
        scale=Math.max(.25f,Math.min(1,Math.min((width-12)/348f,(height-12)/252f)));
        width=(int)(width/scale);height=(int)(height/scale);super.init();categories.clear();types.clear();
        for(int i=0;i<4;i++){
            var category=AmmunitionCategory.values()[i];
            var b=new LineButton(leftPos+8+i*28,topPos+38,26,24,tr("category."+category.path()),button->{
                select(Arrays.stream(AmmunitionType.values()).filter(t->t.category()==category).findFirst().orElseThrow());
            });
            b.category=true;b.iconType=Arrays.stream(AmmunitionType.values()).filter(t->t.category()==category).findFirst().orElseThrow();
            categories.add(addRenderableWidget(b));
        }
        for(int i=0;i<3;i++){
            final int index=i;
            var b=new LineButton(leftPos+8,topPos+70+i*18,112,16,Component.empty(),button->{var entries=entries();if(index<entries.size())select(entries.get(index));});
            types.add(addRenderableWidget(b));
        }
        quantity=new QuantityBox(leftPos+10,topPos+132,28,10);quantity.setBordered(false);quantity.setMaxLength(10);
        quantity.setFilter(s->s.isEmpty()||s.chars().allMatch(Character::isDigit));quantity.setTextColor(TEXT);quantity.setTextColorUneditable(MUTED);
        quantity.setResponder(s->{if(!settingText)dirty=true;});addRenderableWidget(quantity);displayQuantity(menu.browse.quantity);
        run=addRenderableWidget(new LineButton(leftPos+128,topPos+103,116,18,tr("start"),button->{
            if(job()!=null){var t=new CompoundTag();t.putUUID("Job",job().id);action(2,t);}
            else{commitQuantity();var t=new CompoundTag();t.putString("Target",menu.browse.selected.id().toString());t.putInt("Quantity",menu.browse.quantity);action(1,t);}
        }));
        refresh();
    }
    private void refresh(){
        var entries=entries();for(int i=0;i<types.size();i++){
            var b=types.get(i);b.visible=i<entries.size();if(b.visible){b.iconType=entries.get(i);b.setMessage(Component.translatable(b.iconType.translationKey()));b.selected=b.iconType==menu.browse.selected;}
        }
        for(var b:categories)b.selected=b.iconType.category()==menu.browse.category();
        if(run==null)return;
        boolean stopping=job()!=null&&job().stopping;
        run.setMessage(tr(stopping?"stopping_button":job()==null?"start":"interrupt"));
        run.active=minecraft.player!=null&&!minecraft.player.isSpectator()&&!stopping&&(job()!=null||normalized()>0&&menu.factory.canFit(recipe()));
    }
    @Override protected void containerTick(){super.containerTick();if(!quantity.isFocused()&&!dirty&& !quantity.getValue().equals(Integer.toString(menu.browse.quantity)))displayQuantity(menu.browse.quantity);refresh();}
    @Override protected void renderLabels(GuiGraphics g,int x,int y){}
    private String shortened(String value,int maximum){return font.width(value)<=maximum?value:font.plainSubstrByWidth(value,Math.max(0,maximum-font.width("…")))+"…";}
    private void center(GuiGraphics g,Component value,int x,int y,int color,int maximum){String s=shortened(value.getString(),maximum);g.drawString(font,s,leftPos+x-font.width(s)/2,topPos+y,color,false);}
    private void item(GuiGraphics g,ItemStack stack,int x,int y,float size){
        var id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        var texture=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(id.getNamespace(),"textures/item/"+id.getPath()+".png");int length=(int)size;
        g.flush();com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();g.blit(texture,x,y,0,0,length,length,length,length);com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
    }
    private String metal(long value){
        return com.lex0v0.piranport.text.ManufacturingText.metalParts(value,tr("unit_ingot").getString(),tr("unit_nugget").getString());
    }
    private long demandRuns(){var r=recipe();return r==null?0:menu.browse.total?Math.max(0,Math.min(Integer.MAX_VALUE,input())+r.count()-1)/r.count():1;}
    @Override protected void renderBg(GuiGraphics g,float tick,int mx,int my){
        ManufacturingGuiStyle.shell(g,leftPos,topPos,imageWidth,font.width(title));g.drawString(font,title,leftPos+8,topPos+6,TEXT,false);
        g.drawString(font,tr("categories"),leftPos+8,topPos+26,MUTED,false);
        ManufacturingGuiStyle.frame(g,leftPos+8,topPos+128,32,16);
        g.drawString(font,tr("pieces_unit"),leftPos+43,topPos+132,MUTED,false);
        int track=leftPos+(minecraft.getLanguageManager().getSelected().startsWith("zh")?59:65);
        int length=leftPos+120-track;
        g.fill(track,topPos+134,track+length,topPos+138,0xff0a2032);ManufacturingGuiStyle.outline(g,track,topPos+133,length,6,0xff345f7a);
        var r=recipe();int maximum=menu.factory.maximum(r);int offset=maximum==0?0:(int)((length-7)*(double)Math.min(maximum,Math.max(0,input()))/maximum);
        g.fill(track+1,topPos+134,track+offset+3,topPos+138,0xff24799f);ManufacturingGuiStyle.button(g,track+offset,topPos+129,7,14,maximum>0,false);
        ManufacturingGuiStyle.frame(g,leftPos+128,topPos+24,116,62);
        g.drawString(font,tr(menu.browse.total?"total":"single"),leftPos+134,topPos+28,MUTED,false);
        g.fill(leftPos+134,topPos+38,leftPos+238,topPos+39,0xff4b6578);
        int switchX=leftPos+DEMAND_SWITCH_X;
        g.fill(switchX,topPos+30,switchX+DEMAND_SWITCH_WIDTH,topPos+34,0xff0a2032);ManufacturingGuiStyle.outline(g,switchX,topPos+29,DEMAND_SWITCH_WIDTH,6,0xff345f7a);
        if(menu.browse.total)g.fill(switchX+1,topPos+30,switchX+DEMAND_SWITCH_WIDTH-4,topPos+34,0xff24799f);
        ManufacturingGuiStyle.button(g,switchX+(menu.browse.total?DEMAND_SWITCH_WIDTH-DEMAND_SWITCH_THUMB:0),topPos+26,DEMAND_SWITCH_THUMB,12,true,true);
        if(r!=null){
            int row=0;
            for(int i=0;i<4;i++){
                String name=AmmunitionMaterials.MATERIALS.get(i);long amount=r.cost(name)*demandRuns();if(amount==0)continue;
                int y=topPos+42+row++*10;
                Component label=tr("material."+name);g.drawString(font,label,leftPos+146,y,TEXT,false);
                String value=i==3?amount+tr("unit_powder").getString():metal(amount);
                value=shortened(value,Math.max(0,88-font.width(label)-4));g.drawString(font,value,leftPos+238-font.width(value),y,TEXT,false);
            }
            center(g,tr("batch",r.count(),r.seconds()),186,90,MUTED,116);
        }else center(g,tr("no_recipe"),186,90,MUTED,116);
        drawTask(g);drawArrow(g);
        center(g,tr("auto_convert"),296,26,MUTED,88);
        for(int row=0;row<2;row++)for(int col=0;col<4;col++){
            ManufacturingGuiStyle.slot(g,leftPos+260+col*18,topPos+AmmunitionFactoryMenu.INPUT_Y+row*18);
            ManufacturingGuiStyle.slot(g,leftPos+260+col*18,topPos+AmmunitionFactoryMenu.OUTPUT_Y+row*18);
        }
        g.fill(leftPos+8,topPos+151,leftPos+340,topPos+152,ManufacturingGuiStyle.DIVIDER);
        g.drawString(font,playerInventoryTitle,leftPos+93,topPos+157,MUTED,false);
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)ManufacturingGuiStyle.slot(g,leftPos+93+col*18,topPos+170+row*18);
        for(int col=0;col<9;col++)ManufacturingGuiStyle.slot(g,leftPos+93+col*18,topPos+228);
    }
    private void drawTask(GuiGraphics g){
        var job=job();if(job==null){center(g,Component.translatable(menu.browse.selected.translationKey()).copy().append(tr("idle_suffix")),186,126,TEXT,116);var r=recipe();center(g,tr("planned",Math.max(0,input()),r==null?0:demandRunsForInput(r)),186,140,MUTED,116);return;}
        String state=menu.factory.status();
        String title=state.equals("running")?shortened(Component.translatable(job.recipe.ammunition().translationKey()).getString(),116-font.width(tr("running_suffix")))+tr("running_suffix").getString():tr("state."+state).getString();
        center(g,Component.literal(title),186,126,ORANGE,116);
        Component counts=job.stopping?tr("stop_counts",job.completedPieces(),(job.completedRuns+1)*job.recipe.count()):tr("counts",job.completedPieces(),job.totalPieces(),job.completedRuns+1,job.totalRuns);
        center(g,counts,186,140,MUTED,116);
    }
    private long demandRunsForInput(AmmunitionFactoryRecipe r){return(Math.max(0,Math.min(Integer.MAX_VALUE,input()))+r.count()-1)/r.count();}
    private void drawIcons(GuiGraphics g){
        for(var b:categories)item(g,new ItemStack(ModItems.ammunition(b.iconType)),b.getX()+7,b.getY()+2,12);
        for(var b:types)if(b.visible)item(g,new ItemStack(ModItems.ammunition(b.iconType)),b.getX()+2,b.getY()+1,14);
        var r=recipe();if(r==null)return;
        var icons=List.of(new ItemStack(Items.COPPER_INGOT),new ItemStack(ModItems.STEEL_INGOT.get()),new ItemStack(ModItems.ALUMINUM_INGOT.get()),new ItemStack(Items.GUNPOWDER));int row=0;
        for(int i=0;i<4;i++)if(r.cost(AmmunitionMaterials.MATERIALS.get(i))*demandRuns()>0)item(g,icons.get(i),leftPos+134,topPos+42+row++*10,8);
    }
    private void drawArrow(GuiGraphics g){
        int fill=job()==null||!job().inFlight?0:(int)(20L*job().elapsed/job().recipe.ticks());
        for(int row=0;row<20;row++){
            int w=row<10?10:20-2*(row-10),x=leftPos+296-w/2,y=topPos+AmmunitionFactoryMenu.ARROW_Y+row;
            g.fill(x,y,x+w,y+1,ManufacturingGuiStyle.SELECTED);
            if(row>0&&row<19&&w>2)g.fill(x+1,y,x+w-1,y+1,row<fill?0xff27b1d5:0xff0a2235);
        }
    }
    private boolean inside(double x,double y,int a,int b,int w,int h){return x>=leftPos+a&&x<leftPos+a+w&&y>=topPos+b&&y<topPos+b+h;}
    private void slide(double x){var r=recipe();if(r==null)return;int start=leftPos+(minecraft.getLanguageManager().getSelected().startsWith("zh")?59:65),length=leftPos+120-start-7;int maximum=menu.factory.maximum(r);
        int wanted=maximum==0?0:Math.max(r.count(),(int)Math.round(Math.clamp((x-start)/Math.max(1,length),0,1)*maximum));menu.browse.quantity=AmmunitionMaterials.normalize(wanted,r.count(),maximum);displayQuantity(menu.browse.quantity);browseChanged(true);}
    @Override public boolean mouseClicked(double x,double y,int button){
        x/=scale;y/=scale;if(quantity.isFocused()&&!quantity.isMouseOver(x,y))commitQuantity();
        if(button==0&&inside(x,y,DEMAND_SWITCH_X,26,DEMAND_SWITCH_WIDTH,12)){menu.browse.total=!menu.browse.total;browseChanged(false);return true;}
        if(button==0&&inside(x,y,59,128,61,16)){dragging=true;slide(x);return true;}
        return super.mouseClicked(x,y,button);
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){if(dragging){slide(x/scale);return true;}return super.mouseDragged(x/scale,y/scale,button,dx/scale,dy/scale);}
    @Override public boolean mouseReleased(double x,double y,int button){dragging=false;return super.mouseReleased(x/scale,y/scale,button);}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){return super.mouseScrolled(x/scale,y/scale,horizontal,vertical);}
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(quantity.isFocused()&&key!=GLFW.GLFW_KEY_ESCAPE){if(key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER)commitQuantity();else quantity.keyPressed(key,scan,modifiers);return true;}
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public void onClose(){commitQuantity();super.onClose();}
    @Override public void render(GuiGraphics g,int x,int y,float tick){
        // 先提交HUD并清理深度，避免世界或前一层GUI遮挡物品图标。
        g.flush();com.mojang.blaze3d.systems.RenderSystem.depthMask(true);com.mojang.blaze3d.systems.RenderSystem.clear(org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT,net.minecraft.client.Minecraft.ON_OSX);
        int mx=(int)(x/scale),my=(int)(y/scale);g.pose().pushPose();g.pose().scale(scale,scale,1);super.render(g,mx,my,tick);drawIcons(g);renderTooltip(g,mx,my);
        if(inside(mx,my,128,24,116,62)&&recipe()!=null){
            List<Component> lines=new ArrayList<>();lines.add(tr(menu.browse.total?"total":"single"));
            for(String name:AmmunitionMaterials.MATERIALS){long value=recipe().cost(name)*demandRuns();if(value>0)lines.add(tr("material."+name).copy().append(": "+(name.equals("gunpowder")?value+tr("unit_powder").getString():metal(value))));}
            g.renderComponentTooltip(font,lines,mx,my);
        }else if(inside(mx,my,8,128,112,16))g.renderTooltip(font,tr("maximum",menu.factory.maximum(recipe())),mx,my);
        else for(var b:types)if(b.visible&&b.isMouseOver(mx,my))g.renderTooltip(font,b.getMessage(),mx,my);
        g.pose().popPose();
    }
    // 短数字连同光标和选区居中，长输入保留原版横向滚动与点击定位。
    private final class QuantityBox extends EditBox {
        QuantityBox(int x,int y,int w,int h){super(AmmunitionFactoryScreen.this.font,x,y,w,h,tr("quantity"));}
        private int textOffset(){return Math.max(0,(getInnerWidth()-font.width(getValue()))/2);}
        @Override public void renderWidget(GuiGraphics g,int mx,int my,float tick){int offset=textOffset();g.pose().pushPose();g.pose().translate(offset,0,0);super.renderWidget(g,mx-offset,my,tick);g.pose().popPose();}
        @Override public void onClick(double x,double y){super.onClick(x-textOffset(),y);}
    }
    private final class LineButton extends Button {
        private AmmunitionType iconType;
        private boolean category,selected;
        LineButton(int x,int y,int w,int h,Component text,OnPress press){super(x,y,w,h,text,press,DEFAULT_NARRATION);}
        @Override protected void renderWidget(GuiGraphics g,int mx,int my,float tick){
            ManufacturingGuiStyle.button(g,getX(),getY(),width,height,active,selected||isHoveredOrFocused());
            if(category){String s=getMessage().getString();g.drawString(font,s,getX()+(width-font.width(s))/2,getY()+15,selected?TEXT:MUTED,false);}
            else if(iconType!=null){g.drawString(font,shortened(getMessage().getString(),width-26),getX()+20,getY()+4,TEXT,false);}
            else {String s=shortened(getMessage().getString(),width-8);g.drawString(font,s,getX()+(width-font.width(s))/2,getY()+(height-8)/2,active?TEXT:MUTED,false);}
        }
    }
}
