package com.lex0v0.piranport.client;

import static com.lex0v0.piranport.text.ManufacturingText.metal;
import static com.lex0v0.piranport.text.ManufacturingText.time;

import com.lex0v0.piranport.text.ClassificationText;
import com.lex0v0.piranport.client.gui.ManufacturingGuiStyle;

import com.lex0v0.piranport.blockentity.DevelopmentFactoryBlockEntity;
import com.lex0v0.piranport.blockentity.developmentfactory.FactoryJob;
import com.lex0v0.piranport.blockentity.manufacturing.ManufacturingMaterials;
import com.lex0v0.piranport.blockentity.developmentfactory.FactoryFilter;
import com.lex0v0.piranport.client.developmentfactory.FactoryFilterDialog;
import com.lex0v0.piranport.blockentity.developmentfactory.FactoryEntry;
import com.lex0v0.piranport.blockentity.developmentfactory.FactoryCatalog;
import com.lex0v0.piranport.domain.equipment.EquipmentDefinitions;
import com.lex0v0.piranport.item.AmmunitionItem;

import com.lex0v0.piranport.menu.DevelopmentFactoryMenu;
import com.lex0v0.piranport.network.DevelopmentFactoryActionPayload;
import com.lex0v0.piranport.recipe.DevelopmentFactoryRecipe;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import net.minecraft.util.FormattedCharSequence;

// 沿用舰R定稿布局，默认字体随整体GUI等比适配小窗口。
public final class DevelopmentFactoryScreen extends AbstractContainerScreen<DevelopmentFactoryMenu> {
    private static final int WHITE = ManufacturingGuiStyle.TEXT, MUTED = ManufacturingGuiStyle.MUTED, ORANGE = 0xffedb05d, GREEN = 0xff8acb98;
    private final List<FactoryButton> tabs = new ArrayList<>();
    private FactoryButton filter, search, build, confirm, back;
    private FactoryFilterDialog filterDialog;
    private UUID confirmation;
    private int confirmationWidth, confirmationButtonY;
    private float screenScale = 1;
    private boolean draggingScrollbar;
    private boolean focusReturnOnNextTick;

    public DevelopmentFactoryScreen(DevelopmentFactoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 384; imageHeight = 252;
    }

    private static Component tr(String key, Object... args) { return Component.translatable("gui.piranport.development_factory." + key, args); }
    private void action(int kind, CompoundTag data) { PacketDistributor.sendToServer(new DevelopmentFactoryActionPayload(menu.containerId, kind, data)); }
    private void browseChanged() { menu.normalizeBrowse(); action(0, menu.browse.save()); }
    private FactoryJob job() { return menu.factory.job(); }
    private FactoryEntry selected() { var id = menu.browse.selected; return id == null ? null : FactoryCatalog.byId(id); }
    private DevelopmentFactoryRecipe recipe() { var entry = selected(); return entry == null ? null : menu.factory.recipe(entry.id()); }

    @Override protected void init() {
        FactoryFilter draft = filterDialog == null ? null : filterDialog.value();
        filterDialog = null;
        screenScale = Math.min(1, Math.min((width - 12) / 384f, (height - 12) / 280f));
        screenScale = Math.max(.25f, screenScale);
        width = (int) (width / screenScale); height = (int) (height / screenScale);
        super.init(); topPos = Math.max(25, topPos);
        tabs.clear();
        for (int i = 0; i < 2; i++) {
            var tab = new FactoryButton(leftPos + 8 + i * 28, topPos - 24, 26, 25,
                    tr(new String[]{"tab_build", "tab_repair"}[i]), b -> {}, i);
            tabs.add(addRenderableWidget(tab));
        }
        filter = addRenderableWidget(new FactoryButton(leftPos + 112, topPos + 24, 116, 12, tr("all"), b -> {
            menu.browse.all = !menu.browse.all; menu.browse.scroll = 0; browseChanged();
        }, 3));
        search = addRenderableWidget(new FactoryButton(leftPos + 228, topPos + 24, 12, 12, tr("search"), b -> openFilter(menu.browse.filter), 4));
        build = addRenderableWidget(new FactoryButton(leftPos + 248, topPos + 90, 128, 16, tr("build"), b -> {
            if (job() != null) {
                confirmation = job().id; layoutConfirmation(); refreshButtons(); focusReturnOnNextTick = true;
            } else {
                CompoundTag data = new CompoundTag();
                if (selected() != null) data.putString("Target", selected().id().toString());
                action(1, data);
            }
        }, -1));
        confirm = addWidget(new FactoryButton(0, 0, 64, 18, tr("confirm_cancel"), b -> {
            if (confirmation != null) {
                CompoundTag data = new CompoundTag(); data.putUUID("Job", confirmation);
                action(2, data); dismissConfirmation();
            }
        }, -1));
        back = addWidget(new FactoryButton(0, 0, 64, 18, tr("return"), b -> dismissConfirmation(), -1));
        refreshButtons();
        if (confirmation != null) { layoutConfirmation(); setFocused(back); }
        else if (draft != null) openFilter(draft);
    }

    private void dismissConfirmation() { confirmation = null; refreshButtons(); setFocused(build); }
    private void openFilter(FactoryFilter initial) {
        draggingScrollbar = false;
        filterDialog = new FactoryFilterDialog(font, leftPos + imageWidth / 2, topPos + imageHeight / 2, initial,
                menu.browse.all, value -> menu.visibleEntries(value).size(), value -> {
                    menu.browse.filter = value; menu.browse.scroll = 0; browseChanged(); closeFilter();
                }, this::closeFilter);
        filterDialog.widgets().forEach(this::addWidget); refreshButtons(); setFocused(filterDialog.input);
    }

    private void closeFilter() {
        filterDialog.widgets().forEach(this::removeWidget); filterDialog = null;
        refreshButtons(); setFocused(search);
    }

    private void refreshButtons() {
        if (build == null) return;
        boolean modal = confirmation != null || filterDialog != null;
        tabs.forEach(tab -> tab.active = !modal && tab.kind == 0); filter.active = search.active = !modal;
        boolean output = !menu.factory.stack(DevelopmentFactoryBlockEntity.OUTPUT_SLOT).isEmpty();
        build.setMessage(job() == null ? tr("build") : tr("cancel"));
        build.active = !modal && minecraft.player != null && !minecraft.player.isSpectator() && (job() != null || output || (selected() != null
                && menu.factory.permitted(selected().id()) && menu.factory.affordable(recipe())));
        confirm.visible = back.visible = confirmation != null; confirm.active = back.active = confirmation != null;
    }

    @Override protected void containerTick() {
        super.containerTick(); menu.normalizeBrowse();
        if (confirmation != null) {
            var actual = menu.factory.job();
            if (actual == null || !actual.id.equals(confirmation)) dismissConfirmation();
        }
        refreshButtons();
        if (focusReturnOnNextTick && confirmation != null) { setFocused(back); focusReturnOnNextTick = false; }
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}

    @Override protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        ManufacturingGuiStyle.background(g, leftPos, topPos, imageWidth, font.width(title), 48);
        g.drawString(font,title,leftPos+8,topPos+7,WHITE,false);
        g.drawString(font,playerInventoryTitle,leftPos+168,topPos+152,WHITE,false);
        center(g,tr("fuel"),54,224,MUTED);center(g,tr("cache"),108,224,MUTED);
        center(g,Component.literal(menu.factory.fuel().getAmount()+" / 16000 mB"),86,236,WHITE);
        int fuelHeight=Math.clamp((int)(menu.factory.fuel().getAmount()*50L/16000),0,50);
        ManufacturingGuiStyle.fuel(g, leftPos + 49, topPos + 166, fuelHeight);
        drawModel(g);
        drawList(g);
        drawRequirements(g);
        drawProgress(g);
    }

    private void center(GuiGraphics g, Component text, int x, int y, int color) { g.drawString(font, text, leftPos + x - font.width(text) / 2, topPos + y, color, false); }
    private String shorten(String value, int width) {
        if (font.width(value) <= width) return value;
        return font.plainSubstrByWidth(value, Math.max(0, width - font.width("…"))) + "…";
    }

    private Component name(FactoryEntry entry, String suffix, int width) {
        Component tail = suffix.isEmpty() ? Component.empty() : tr(suffix).copy().withStyle(suffix.equals("building") ? ChatFormatting.GOLD : ChatFormatting.GREEN);
        return Component.literal(shorten(Component.translatable(entry.translationKey()).getString(), Math.max(0, width - font.width(tail)))).append(tail);
    }

    private String stateFor(FactoryEntry entry) {
        if (job() != null && job().recipe.target().equals(entry.id())) return "building";
        return FactoryCatalog.definition(menu.factory.stack(DevelopmentFactoryBlockEntity.OUTPUT_SLOT)).filter(value -> value.id().equals(entry.id())).isPresent() ? "pending" : "";
    }

    private FactoryEntry preview() {
        FactoryEntry preview=selected();
        if(preview==null)preview=FactoryCatalog.definition(menu.factory.stack(DevelopmentFactoryBlockEntity.OUTPUT_SLOT)).orElse(null);
        return preview;
    }

    private Component classification(FactoryEntry entry) {
        return EquipmentDefinitions.find(entry.id()).map(ClassificationText::equipment)
                .orElseGet(() -> ClassificationText.equipment(entry.category()));
    }

    private void drawModel(GuiGraphics g){
        FactoryEntry preview=preview();
        if(preview==null)return;
        g.pose().pushPose();g.pose().translate(leftPos+40,topPos+32,0);g.pose().scale(2,2,1);g.renderItem(preview.create(1),0,0);g.pose().popPose();
        center(g,name(preview,"",96),56,80,WHITE);
        var definition=EquipmentDefinitions.find(preview.id());
        Component category=classification(preview);
        center(g,Component.literal(shorten(category.getString(),96)),56,94,MUTED);
        var recipe=menu.factory.recipe(preview.id());
        if(definition.isPresent()){
            var value=definition.get();center(g,tr("durability",value.maxDurability()),56,108,MUTED);
            if(value.fuelCapacity()>0)center(g,tr("fuel_capacity",value.fuelCapacity()),56,122,MUTED);
        }else if(recipe!=null&&recipe.count()>1)center(g,tr("output_count",recipe.count()),56,108,MUTED);
        if(recipe==null)center(g,Component.literal(shorten(tr("no_recipe").getString(),96)),56,122,MUTED);
    }

    private void drawList(GuiGraphics g) {
        List<FactoryEntry> entries = menu.visibleEntries(); int start = menu.browse.scroll;
        if (entries.isEmpty() && menu.browse.filter.active()) center(g, tr("no_matches"), 172, 80, MUTED);
        for (int row = 0; row < 4 && start + row < entries.size(); row++) {
            var entry = entries.get(start + row); int x = leftPos + 113, y = topPos + 38 + row * 24;
            boolean available = menu.factory.permitted(entry.id()) && menu.factory.affordable(menu.factory.recipe(entry.id()));
            boolean selected = entry.id().equals(menu.browse.selected);
            ManufacturingGuiStyle.listRow(g, x, y, available, selected);
            g.renderItem(entry.create(1),x+2,y+3);
            g.drawString(font, name(entry, stateFor(entry), 96), x + 19, y + 3, available ? WHITE : MUTED, false);
            var recipe = menu.factory.recipe(entry.id()); String time = recipe == null ? "--:--" : time(recipe.ticks());
            Component type = classification(entry);
            g.drawString(font, shorten(type.getString(), 92 - font.width(time)), x + 19, y + 13, MUTED, false);
            g.drawString(font, time, x + 115 - font.width(time), y + 13, MUTED, false);
            if (stateFor(entry).equals("building")) g.fill(x, y, x + 1, y + 23, ORANGE);
        }
        g.fill(leftPos + 233, topPos + 38, leftPos + 238, topPos + 134, 0xff142737);
        int thumb = entries.size() <= 4 ? 96 : Math.max(12, 96 * 4 / entries.size());
        int top = entries.size() <= 4 ? 0 : (96 - thumb) * start / (entries.size() - 4);
        g.fill(leftPos + 234, topPos + 38 + top, leftPos + 237, topPos + 38 + top + thumb, 0xff617d91);
    }

    private void drawRequirements(GuiGraphics g) {
        g.drawString(font, tr("requirements"), leftPos + 253, topPos + 28, MUTED, false);
        g.fill(leftPos + 252, topPos + 38, leftPos + 372, topPos + 39, 0xff4b6578);
        var recipe = recipe(); if (recipe == null) return;
        var cache = menu.factory.materials();
        long[] available = {menu.factory.fuel().getAmount(), ManufacturingMaterials.ammunition(cache), ManufacturingMaterials.available(cache, "steel"), ManufacturingMaterials.available(cache, "aluminum")};
        int[] required = {recipe.fuel(), recipe.ammunition(), recipe.steel() * 9, recipe.aluminum() * 9};
        String[] keys = {"fuel", "ammunition", "steel", "aluminum"};
        for (int row = 0; row < 4; row++) {
            int y = topPos + 42 + row * 10; boolean missing = available[row] < required[row];
            if (missing) g.fill(leftPos + 252, y - 1, leftPos + 372, y + 8, 0xff50313b);
            int color = missing ? 0xffec7979 : WHITE;
            g.drawString(font, tr(keys[row]), leftPos + 253, y, color, false);
            String need = Long.toString(row >= 2 ? required[row] / 9 : required[row]);
            String have = row >= 2 ? metal(available[row]) : Long.toString(available[row]);
            String unit = row == 0 ? "mB" : tr(row == 1 ? "unit_points" : "unit_ingots").getString();
            String counts = shorten(need + " / " + have + " " + unit, Math.min(94,114-font.width(tr(keys[row]))-5));
            g.drawString(font, counts, leftPos + 372 - font.width(counts), y, color, false);
        }
    }

    private void drawProgress(GuiGraphics g) {
        int x = leftPos + 272, y = topPos + 124;
        int fill = job() == null ? 0 : (int) (68L * job().elapsed() / job().recipe.ticks());
        g.fill(x, y + 4, x + 69, y + 9, 0xff1b425d);
        if (fill > 0) g.fillGradient(x, y + 4, x + fill, y + 9, 0xff4bd2ef, 0xff158fbd);
        g.fill(x, y + 3, x + 69, y + 4, ManufacturingGuiStyle.SELECTED); g.fill(x, y + 9, x + 69, y + 10, ManufacturingGuiStyle.SELECTED);
        for (int row = 0; row < 15; row++) {
            int right = 78 - Math.abs(row - 7);
            g.fill(x + 68, y + row - 1, x + right, y + row, ManufacturingGuiStyle.SELECTED);
            if (right > 70) g.fill(x + 69, y + row - 1, x + right - 1, y + row, 0xff1b425d);
        }
        FactoryEntry task = job() != null ? FactoryCatalog.byId(job().recipe.target()) : FactoryCatalog.definition(menu.factory.stack(DevelopmentFactoryBlockEntity.OUTPUT_SLOT)).orElse(null);
        if (task != null) center(g, name(task, job() != null ? "building" : "pending", 128), 312, 110, WHITE);
    }

    private void outline(GuiGraphics g, int x, int y, int w, int h, int c) {
        g.fill(x,y,x+w,y+1,c);g.fill(x,y+h-1,x+w,y+h,c);g.fill(x,y,x+1,y+h,c);g.fill(x+w-1,y,x+w,y+h,c);
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int mx = (int) (mouseX / screenScale), my = (int) (mouseY / screenScale);
        g.pose().pushPose(); g.pose().scale(screenScale, screenScale, 1);
        super.render(g, mx, my, partialTick);
        if (confirmation != null) {
            g.pose().pushPose(); g.pose().translate(0, 0, 400);
            drawConfirmation(g, mx, my, partialTick); g.pose().popPose();
        } else if (filterDialog != null) {
            g.pose().pushPose(); g.pose().translate(0, 0, 400);
            g.fill(0,0,width,height,0xb0000000); filterDialog.render(g,mx,my,partialTick); g.pose().popPose();
        }
        g.pose().popPose();
        if (filterDialog != null) {
            var lines=filterDialog.tooltip(mx,my);
            if(!lines.isEmpty())g.renderComponentTooltip(font,lines,mouseX,mouseY);
        } else if (confirmation == null) {
            if (hoveredSlot != null && hoveredSlot.index == DevelopmentFactoryBlockEntity.BLUEPRINT_SLOT && job() != null) {
                var stack = hoveredSlot.getItem();
                var lines = new ArrayList<>(Screen.getTooltipFromItem(minecraft, stack));
                lines.add(tr("reserved")); lines.add(tr("refill"));
                g.renderTooltip(font, lines, stack.getTooltipImage(), stack, mouseX, mouseY);
            } else { renderTooltip(g, mouseX, mouseY); drawTooltips(g, mx, my, mouseX, mouseY); }
        }
    }

    private boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= leftPos + left && x < leftPos + left + width && y >= topPos + top && y < topPos + top + height;
    }

    private void drawTooltips(GuiGraphics g, int x, int y, int screenX, int screenY) {
        List<Component> lines = new ArrayList<>();
        if (inside(x,y,8,-24,54,24)) {
            int index = (x - leftPos - 8) / 28;
            if (index < 2) { lines.add(tabs.get(index).getMessage()); if (index > 0) lines.add(tr("locked")); }
        } else if (inside(x,y,228,24,12,12)) {
            lines.add(tr(menu.browse.filter.active() ? "filter_active" : "search"));
            lines.add(tr("filter_left"));
            if(menu.browse.filter.active())lines.add(tr("filter_right"));
        } else if (inside(x,y,47,164,14,54)) lines.add(tr("fuel_controls"));
        else if (inside(x,y,248,122,18,18) && job() != null) { lines.add(tr("reserved")); lines.add(tr("refill")); }
        else if (inside(x,y,272,122,80,18) && job() != null) {
            var task = job(); var entry = FactoryCatalog.byId(task.recipe.target());
            if (entry != null) { lines.add(Component.translatable(entry.translationKey()).append(tr("building"))); lines.add(classification(entry)); }
            lines.add(tr("progress", time(task.elapsed()), time(task.recipe.ticks()), time(task.recipe.ticks()-task.elapsed())));
            lines.add(tr("spent")); lines.add(tr("refund_metals", task.recipe.steel(), task.recipe.aluminum()));
            lines.add(Component.literal(task.fuel.getAmount()+" mB"));
            task.ammunition.forEach(stack -> lines.add(stack.getHoverName().copy().append(" ×"+stack.getCount())));
        } else if (inside(x,y,248,108,128,12)) {
            var entry = job() == null ? FactoryCatalog.definition(menu.factory.stack(DevelopmentFactoryBlockEntity.OUTPUT_SLOT)).orElse(null) : FactoryCatalog.byId(job().recipe.target());
            if (entry != null) { lines.add(Component.translatable(entry.translationKey()).append(tr(job() == null ? "pending" : "building"))); lines.add(classification(entry)); }
        } else if (inside(x,y,8,24,96,112) && preview() != null) {
            lines.add(Component.translatable(preview().translationKey())); lines.add(classification(preview()));
        }
        else if (inside(x,y,113,38,119,96)) {
            int row = (y-topPos-38)/24 + menu.browse.scroll; var entries=menu.visibleEntries();
            if (row < entries.size()) { var entry=entries.get(row); MutableComponent line=Component.translatable(entry.translationKey());String state=stateFor(entry);if(!state.isEmpty())line.append(tr(state));lines.add(line);lines.add(classification(entry)); }
        } else if (inside(x,y,252,41,120,40) && recipe() != null) {
            int row=Math.clamp((y-topPos-41)/10,0,3);var cache=menu.factory.materials();var value=recipe();
            String[] keys={"fuel","ammunition","steel","aluminum"};
            long have=switch(row){case 0->menu.factory.fuel().getAmount();case 1->ManufacturingMaterials.ammunition(cache);default->ManufacturingMaterials.available(cache,keys[row])/9;};
            int need=switch(row){case 0->value.fuel();case 1->value.ammunition();case 2->value.steel();default->value.aluminum();};
            lines.add(tr(keys[row]).copy().append(": "+need+" / "+have+" ").append(row==0?Component.literal("mB"):tr(row==1?"unit_points":"unit_ingots")));
            if(row==1){var plan=ManufacturingMaterials.plan(cache,value);if(plan!=null){int points=plan.ammunition().stream().mapToInt(stack->AmmunitionItem.points(stack)*stack.getCount()).sum();lines.add(tr("ammo_budget",value.ammunition(),points));plan.ammunition().forEach(stack->lines.add(stack.getHoverName().copy().append(" ×"+stack.getCount())));}}
        }
        if (!lines.isEmpty()) g.renderComponentTooltip(font, lines, screenX, screenY);
    }


    private List<FormattedCharSequence> confirmationLines(int width) {
        List<Component> lines=new ArrayList<>(); var task=job();
        if(task==null)return List.of();
        var entry=FactoryCatalog.byId(task.recipe.target());
        lines.add(entry==null?Component.literal(task.recipe.target().toString()):Component.translatable(entry.translationKey()));
        lines.add(tr("refund_metals",task.recipe.steel(),task.recipe.aluminum()));
        MutableComponent ammo=Component.empty();
        for(var stack:task.ammunition){if(!ammo.getString().isEmpty())ammo.append(", ");ammo.append(stack.getHoverName()).append(" ×"+stack.getCount());}
        lines.add(tr("refund_ammo",ammo));
        int loss=menu.factory.destroyedFuelOnCancel();
        lines.add(tr("refund_fuel",task.fuel.getAmount()-loss));
        if(loss>0)lines.add(tr("fuel_loss",loss).copy().withStyle(ChatFormatting.RED));
        return lines.stream().flatMap(line -> font.split(line,width).stream()).toList();
    }

    private void layoutConfirmation() {
        int buttonWidth=Math.max(56,Math.max(font.width(tr("confirm_cancel")),font.width(tr("return")))+16);
        confirm.setWidth(buttonWidth);back.setWidth(buttonWidth);
        int contentWidth=Math.max(font.width(tr("confirm")),confirmationLines(216).stream().mapToInt(font::width).max().orElse(0));
        confirmationWidth=Math.max(2*buttonWidth+8+24,Math.min(240,contentWidth+24));
        int height=confirmationHeight();
        confirmationButtonY=topPos+(imageHeight-height)/2+height-28;
        confirm.setPosition(leftPos+(imageWidth-8)/2-buttonWidth,confirmationButtonY);
        back.setPosition(leftPos+(imageWidth+8)/2,confirmationButtonY);
    }

    private int confirmationHeight() {
        return 10+font.split(tr("confirm"),confirmationWidth-24).size()*11+8+confirmationLines(confirmationWidth-24).size()*11+10+18+10;
    }

    private void drawConfirmation(GuiGraphics g, int mx, int my, float tick) {
        g.fill(0,0,width,height,0xb0000000);
        int h=confirmationHeight(),x=leftPos+(imageWidth-confirmationWidth)/2,y=confirmationButtonY+28-h;
        ManufacturingGuiStyle.modal(g, x, y, confirmationWidth, h);
        int at=y+10;
        for(var line:font.split(tr("confirm"),confirmationWidth-24)){g.drawString(font,line,x+(confirmationWidth-font.width(line))/2,at,WHITE,false);at+=11;}
        at+=8;
        for(var line:confirmationLines(confirmationWidth-24)){g.drawString(font,line,x+(confirmationWidth-font.width(line))/2,at,WHITE,false);at+=11;}
        confirm.render(g,mx,my,tick);back.render(g,mx,my,tick);
    }

    @Override public boolean mouseClicked(double x, double y, int button) {
        x/=screenScale;y/=screenScale;
        if(filterDialog!=null){
            for(AbstractWidget widget:filterDialog.widgets())if(widget.mouseClicked(x,y,button)){
                if(filterDialog!=null)setFocused(widget);
                return true;
            }
            return true;
        }
        if(confirmation!=null){if(confirm.mouseClicked(x,y,button)||back.mouseClicked(x,y,button))return true;return true;}
        if(button==1&&inside(x,y,228,24,12,12)){
            if(menu.browse.filter.active()){
                menu.browse.filter=FactoryFilter.NONE;menu.browse.scroll=0;browseChanged();refreshButtons();
                search.playDownSound(minecraft.getSoundManager());
            }
            return true;
        }
        if((button==0||button==1)&&inside(x,y,47,164,14,54)&&!menu.getCarried().isEmpty()){
            CompoundTag data=new CompoundTag();data.putBoolean("Inject",button==0);action(3,data);return true;
        }
        if(button==0&&inside(x,y,233,38,5,96)){draggingScrollbar=true;scrollTo(y);return true;}
        if(button==0&&inside(x,y,113,38,119,96)){
            int row=(int)(y-topPos-38)/24+menu.browse.scroll;var entries=menu.visibleEntries();
            if(row<entries.size()){menu.browse.selected=entries.get(row).id();browseChanged();refreshButtons();}return true;
        }
        boolean handled=super.mouseClicked(x,y,button);
        if(filterDialog!=null)setFocused(filterDialog.input);
        return handled;
    }

    private void scrollTo(double y){int max=Math.max(0,menu.visibleEntries().size()-4);menu.browse.scroll=Math.clamp((int)Math.round((y-topPos-38)/96*max),0,max);browseChanged();}
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){if(filterDialog!=null){filterDialog.input.mouseDragged(x/screenScale,y/screenScale,button,dx/screenScale,dy/screenScale);return true;}if(confirmation!=null)return true;if(draggingScrollbar){scrollTo(y/screenScale);return true;}return super.mouseDragged(x/screenScale,y/screenScale,button,dx/screenScale,dy/screenScale);}
    @Override public boolean mouseReleased(double x,double y,int button){draggingScrollbar=false;return filterDialog!=null||confirmation!=null||super.mouseReleased(x/screenScale,y/screenScale,button);}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
        if(filterDialog!=null||confirmation!=null)return true;
        if(inside(x/screenScale,y/screenScale,112,24,128,112)){int max=Math.max(0,menu.visibleEntries().size()-4);menu.browse.scroll=Math.clamp(menu.browse.scroll-(int)Math.signum(vertical),0,max);browseChanged();return true;}
        return super.mouseScrolled(x/screenScale,y/screenScale,horizontal,vertical);
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(filterDialog!=null){
            if(key==GLFW.GLFW_KEY_ESCAPE)closeFilter();
            else if(key==GLFW.GLFW_KEY_TAB){
                var widgets=filterDialog.widgets();int at=widgets.indexOf(getFocused());
                setFocused(widgets.get(Math.floorMod(at+(Screen.hasShiftDown()?-1:1),widgets.size())));
            } else if(key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER){
                if(getFocused() instanceof Button button)button.onPress();else filterDialog.apply.onPress();
            } else if(getFocused() instanceof Button button&&key==GLFW.GLFW_KEY_SPACE)button.onPress();
            else if(getFocused()==filterDialog.input)filterDialog.input.keyPressed(key,scan,modifiers);
            return true;
        }
        if(confirmation!=null){
            if(key==GLFW.GLFW_KEY_ESCAPE)dismissConfirmation();
            else if(key==GLFW.GLFW_KEY_TAB||key==GLFW.GLFW_KEY_LEFT||key==GLFW.GLFW_KEY_RIGHT)setFocused(getFocused()==back?confirm:back);
            else if(key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER||key==GLFW.GLFW_KEY_SPACE){if(getFocused()==confirm)confirm.onPress();else back.onPress();}
            return true;
        }
        return super.keyPressed(key,scan,modifiers);
    }

    @Override public boolean charTyped(char character,int modifiers){
        if(filterDialog!=null){if(getFocused()==filterDialog.input)filterDialog.input.charTyped(character,modifiers);return true;}
        return confirmation!=null||super.charTyped(character,modifiers);
    }

    private final class FactoryButton extends Button {
        private final int kind;
        private FactoryButton(int x,int y,int w,int h,Component label,OnPress pressed,int kind){super(x,y,w,h,label,pressed,DEFAULT_NARRATION);this.kind=kind;}
        @Override protected void renderWidget(GuiGraphics g,int mouseX,int mouseY,float tick){
            int x=getX(),y=getY(),w=getWidth(),h=getHeight();
            if(kind>=0&&kind<2){
                ManufacturingGuiStyle.tab(g, x, y, w, h, kind == 0);
                if(kind==0){
                    g.fill(x+7,y+17,x+20,y+20,0xffccdfeb);g.fill(x+10,y+14,x+17,y+17,0xffb1cadc);
                    g.fill(x+6,y+11,x+21,y+14,0xffccdfeb);g.fill(x+5,y+10,x+12,y+12,WHITE);
                    for(int row=0;row<7;row++)g.fill(x+12+row/2,y+5+row,x+14+row/2,y+6+row,0xffb1cadc);
                    g.fill(x+10,y+4,x+17,y+7,WHITE);
                    var task=job();boolean pending=!menu.factory.stack(DevelopmentFactoryBlockEntity.OUTPUT_SLOT).isEmpty();
                    if(task!=null||pending){g.fill(x+20,y+3,x+24,y+7,0xff132637);g.fill(x+21,y+4,x+24,y+7,task!=null?ORANGE:GREEN);}
                } else {
                    int color=0xff8295a4;
                    g.fill(x+10,y+10,x+16,y+20,color);g.fill(x+8,y+10,x+18,y+13,color);g.fill(x+10,y+8,x+16,y+10,color);g.fill(x+12,y+6,x+14,y+8,color);
                    g.fill(x+18,y+14,x+25,y+22,0xff142737);g.fill(x+20,y+12,x+24,y+17,0xff142737);
                    g.fill(x+20,y+16,x+24,y+20,0xffa0aeba);g.fill(x+21,y+14,x+23,y+16,0xffa0aeba);
                }
                if(isHoveredOrFocused()&&active)outline(g,x+1,y+1,w-2,h-2,0xff86b9dc);
                return;
            }
            ManufacturingGuiStyle.button(g, x, y, w, h, active, isHoveredOrFocused());
            if(kind==3){g.fill(x+4,y+3,x+10,y+9,0xff8da9bd);g.fill(x+5,y+4,x+9,y+8,0xff19364c);if(menu.browse.all)g.fill(x+6,y+5,x+9,y+8,0xff65b5ec);g.drawString(font,getMessage(),x+15,y+2,WHITE,false);}
            else if(kind==4){
                boolean filtered=menu.browse.filter.active();int color=filtered?0xff83d3ff:WHITE;
                if(filtered){g.fill(x+1,y+1,x+w-1,y+h-1,0xff245e85);outline(g,x,y,w,h,0xff65b5ec);}
                outline(g,x+3,y+2,5,5,color);g.fill(x+7,y+7,x+9,y+9,color);g.fill(x+9,y+9,x+10,y+10,color);
                if(filtered){g.fill(x+1,y+7,x+6,y+8,color);g.fill(x+2,y+8,x+5,y+9,color);g.fill(x+3,y+9,x+4,y+11,color);}
            }
            else {g.drawString(font,getMessage(),x+(w-font.width(getMessage()))/2,y+(h-8)/2,active?WHITE:MUTED,false);
                for(int base=0;base<2;base++)for(int row=0;row<8;row++)g.fill(x+w-13+base*5-row/2,y+4+row,x+w-10+base*5-row/2,y+5+row,0x704884ad);}
        }
    }
}
