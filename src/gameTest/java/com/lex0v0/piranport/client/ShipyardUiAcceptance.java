package com.lex0v0.piranport.client;

import com.lex0v0.piranport.client.gui.screens.ShipyardScreen;
import com.lex0v0.piranport.client.gui.screens.ShipyardFilterDialog;
import com.lex0v0.piranport.blockentity.shipyard.ShipyardFilter;
import com.lex0v0.piranport.domain.ship.ShipType;
import com.lex0v0.piranport.domain.ship.ShipSize;
import com.lex0v0.piranport.domain.ship.ShipDefinitions;

import com.lex0v0.piranport.logging.ModLog;
import com.google.gson.GsonBuilder;
import com.lex0v0.piranport.PiranPort;
import com.lex0v0.piranport.blockentity.ShipyardBlockEntity;
import com.lex0v0.piranport.blockentity.shipyard.ShipyardBrowseState;
import com.lex0v0.piranport.item.blueprint.BlueprintRules;
import com.lex0v0.piranport.item.blueprint.BlueprintType;
import com.lex0v0.piranport.menu.ShipyardMenu;
import com.lex0v0.piranport.registry.ModBlocks;
import com.lex0v0.piranport.registry.ModFluids;
import com.lex0v0.piranport.registry.ModItems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.lwjgl.glfw.GLFW;

// 独立临时世界验证真实GUI与网络交互，测试夹具不进入发行包。
@EventBusSubscriber(modid = PiranPort.MODID, value = Dist.CLIENT)
public final class ShipyardUiAcceptance {
    private static final BlockPos POS = new BlockPos(0, 1, 0);
    private static final Map<String,Object> CHECKS = new LinkedHashMap<>();
    private static int stage, ticks, delay, cancelButtonY;
    private static volatile boolean seeded, longNameReady;
    private static Path output;
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(PiranPort.MODID,path); }
    private static void require(boolean condition,String message) { if(!condition)throw new IllegalStateException(message);CHECKS.put(message,true); }
    private static void add(ShipyardBlockEntity yard,ItemStack stack) {
        for(int slot=2;slot<14&&!stack.isEmpty();slot++)stack=yard.insert(slot,stack,false);
        if(!stack.isEmpty())throw new IllegalStateException("UI fixture materials overflow");
    }

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!Boolean.getBoolean("piranport.acceptance.shipyardUi")||stage==100)return;
        Minecraft mc=Minecraft.getInstance();
        try {
            if(++ticks>4800)throw new IllegalStateException("Shipyard UI test timed out at stage "+stage);
            if(UiTooltipCapture.pending())return;
            if(delay>0){delay--;return;}
            if(stage==0&&AcceptanceClientStartup.titleScreenReady(mc)){
                output=Path.of(System.getProperty("piranport.acceptance.shipyardUiReport"));Files.createDirectories(output);
                Language.inject(ClientLanguage.loadFrom(mc.getResourceManager(),List.of("en_us","zh_cn"),false));
                String world="shipyard-ui-"+System.currentTimeMillis();
                mc.createWorldOpenFlows().createFreshLevel(world,new LevelSettings(world,GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),
                        new WorldOptions(20261024L,false,false),registry->registry.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),new TitleScreen());
                stage=1;
            } else if(stage==1&&mc.player!=null&&mc.level!=null&&mc.getSingleplayerServer()!=null){
                var uuid=mc.player.getUUID();var server=mc.getSingleplayerServer();server.execute(()->{
                    var level=server.overworld();for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)level.setBlock(new BlockPos(x,0,z),Blocks.STONE.defaultBlockState(),3);
                    level.setBlock(POS,ModBlocks.SHIPYARD.get().defaultBlockState(),3);var yard=(ShipyardBlockEntity)level.getBlockEntity(POS);
                    var blueprint=BlueprintRules.create(BlueprintType.AIRCRAFT_CARRIER,BlueprintType.BATTLESHIP,BlueprintType.BATTLECRUISER,BlueprintType.DESTROYER);blueprint.setCount(8);yard.insert(0,blueprint,false);
                    add(yard,new ItemStack(ModItems.STEEL_INGOT.get(),64));add(yard,new ItemStack(ModItems.ALUMINUM_INGOT.get(),64));add(yard,new ItemStack(ModItems.HIGH_EXPLOSIVE_SHELL.get(),64));
                    yard.fluid(null).fill(new FluidStack(ModFluids.FUEL_OIL.get(),32000),FluidAction.EXECUTE);
                    yard.start(id("lexington_cv2"),null);
                    for(int i=0;i<1800;i++)ShipyardBlockEntity.tick(level,POS,yard.getBlockState(),yard);
                    add(yard,new ItemStack(ModItems.STEEL_INGOT.get(),32));
                    var browse=new ShipyardBrowseState();browse.all=true;browse.selected=id("saratoga");yard.remember(browse);
                    var player=server.getPlayerList().getPlayer(uuid);player.getInventory().setItem(9,new ItemStack(ModItems.ARMOR_PIERCING_SHELL.get(),64));player.teleportTo(.5,1.1,3.5);seeded=true;
                });stage=2;
            } else if(stage==2&&seeded&&mc.level!=null&&mc.level.getBlockEntity(POS) instanceof ShipyardBlockEntity){
                var uuid=mc.player.getUUID();var server=mc.getSingleplayerServer();server.execute(()->{
                    var yard=(ShipyardBlockEntity)server.overworld().getBlockEntity(POS);server.getPlayerList().getPlayer(uuid).openMenu(yard,buffer->{buffer.writeBlockPos(POS);buffer.writeNbt(yard.remembered().save());});
                });stage=3;
            } else if(stage==3&&mc.screen instanceof ShipyardScreen screen){
                var menu=screen.getMenu();require(menu.shipyard.job()!=null&&menu.shipyard.job().recipe.ship().equals(id("lexington_cv2")),"running_task_synchronized");
                require(menu.browse.selected.equals(id("saratoga")),"preview_independent_of_task");
                require(!button(screen,"强化").active&&!button(screen,"改造").active,"future_tabs_locked");
                click(screen,"强化");click(screen,"改造");require(menu.browse.selected.equals(id("saratoga"))&&menu.slots.size()==50,"locked_tabs_do_not_change_single_workstation");
                stage=4;delay=20;
            } else if(stage==4&&mc.screen instanceof ShipyardScreen screen){
                capture(mc,"running");
                float z=scale(mc);int left=(int)((mc.getWindow().getGuiScaledWidth()/z-348)/2),top=Math.max(25,(int)((mc.getWindow().getGuiScaledHeight()/z-252)/2));
                require(!screen.getMenu().shipyard.stack(ShipyardBlockEntity.CACHE_START).isEmpty(),"cache_tooltip_fixture_present");
                UiTooltipCapture.request(screen,output,"shipyard-blueprint-tooltip.png",(int)((left+222)*z),(int)((top+131)*z));
                UiTooltipCapture.request(screen,output,"shipyard-cache-tooltip.png",(int)((left+64)*z),(int)((top+174)*z));
                UiTooltipCapture.request(screen,output,"shipyard-ammunition-cache.png",(int)((left+99)*z),(int)((top+174)*z));
                UiTooltipCapture.request(screen,output,"shipyard-ammunition-player.png",(int)((left+159)*z),(int)((top+174)*z));
                stage=41;delay=3;
            } else if(stage==41&&mc.screen instanceof ShipyardScreen screen){
                String reserved=Component.translatable("gui.piranport.shipyard.reserved").getString();
                require(UiTooltipCapture.TOOLTIP_TEXT.getOrDefault("shipyard-blueprint-tooltip.png",List.of()).contains(reserved),"reserved_hint_on_blueprint");
                var material=UiTooltipCapture.TOOLTIP_TEXT.getOrDefault("shipyard-cache-tooltip.png",List.of());
                require(!material.isEmpty()&&!material.contains(reserved),"material_has_no_reserved_hint");
                var ammunition=screen.getMenu().shipyard.stack(ShipyardBlockEntity.CACHE_START+2);
                var pointLine=Component.translatable("tooltip.piranport.manufacturing.ammunition_points",2).getString();
                var totalLine=Component.translatable("tooltip.piranport.manufacturing.ammunition_total",ammunition.getCount()*2L).getString();
                require(UiTooltipCapture.TOOLTIP_TEXT.getOrDefault("shipyard-ammunition-cache.png",List.of()).containsAll(List.of(pointLine,totalLine)),"ammunition_points_only_in_cache");
                var playerTooltip=UiTooltipCapture.TOOLTIP_TEXT.getOrDefault("shipyard-ammunition-player.png",List.of());
                require(!playerTooltip.isEmpty()&&playerTooltip.stream().noneMatch(t->t.contains("制造材料量")||t.contains("当前堆叠")),"player_ammunition_has_no_custom_tooltip");
                click(screen,"取消建造");stage=5;delay=3;
            } else if(stage==5&&mc.screen instanceof ShipyardScreen screen){
                require(screen.getFocused() instanceof Button b&&b.getMessage().getString().equals("返回"),"cancel_defaults_to_return");capture(mc,"cancel-confirmation");
                require(button(screen,"确认取消").getWidth()<112,"cancel_buttons_fit_content");
                stage=51;
            } else if(stage==51&&mc.screen instanceof ShipyardScreen screen){
                screen.keyPressed(GLFW.GLFW_KEY_ESCAPE,0,0);click(screen,"取消建造");cancelButtonY=button(screen,"确认取消").getY();
                mc.getSingleplayerServer().execute(()->{var yard=(ShipyardBlockEntity)mc.getSingleplayerServer().overworld().getBlockEntity(POS);yard.fluid(null).fill(new FluidStack(ModFluids.FUEL_OIL.get(),5000),FluidAction.EXECUTE);});
                stage=6;delay=12;
            } else if(stage==6&&mc.screen instanceof ShipyardScreen screen){
                require(screen.getMenu().shipyard.destroyedFuelOnCancel()==5000&&button(screen,"确认取消").getY()==cancelButtonY,"fuel_warning_does_not_move_confirm_button");capture(mc,"cancel-fuel-overflow");
                stage=61;
            } else if(stage==61&&mc.screen instanceof ShipyardScreen screen){
                click(screen,"确认取消");stage=7;delay=12;
            } else if(stage==7&&mc.screen instanceof ShipyardScreen screen&&screen.getMenu().shipyard.job()==null){
                require(screen.getMenu().shipyard.fuel().getAmount()==32000&&screen.getMenu().shipyard.stack(0).getCount()==8,"confirmed_cancel_roundtrip_preserves_blueprints");
                mc.getSingleplayerServer().execute(()->{
                    var yard=(ShipyardBlockEntity)mc.getSingleplayerServer().overworld().getBlockEntity(POS);yard.start(id("glowworm"),null);
                    for(int i=0;i<900;i++)ShipyardBlockEntity.tick(yard.getLevel(),POS,yard.getBlockState(),yard);
                    yard.extract(0,64,false);
                });click(screen,"显示全部");stage=8;delay=20;
            } else if(stage==8&&mc.screen instanceof ShipyardScreen screen&&!screen.getMenu().shipyard.stack(1).isEmpty()){
                require(button(screen,"建造").active,"pending_output_build_button_clickable");click(screen,"建造");capture(mc,"output-pending");
                stage=81;
            } else if(stage==81&&mc.screen instanceof ShipyardScreen screen){
                mc.getSingleplayerServer().execute(()->{var yard=(ShipyardBlockEntity)mc.getSingleplayerServer().overworld().getBlockEntity(POS);yard.items(net.minecraft.core.Direction.DOWN).extractItem(0,1,false);});
                stage=9;delay=20;
            } else if(stage==9&&mc.screen instanceof ShipyardScreen screen&&screen.getMenu().shipyard.stack(1).isEmpty()){
                require(screen.getMenu().visibleShips().isEmpty(),"no_blueprint_default_list_empty");require(button(screen,"显示全部").active,"no_blueprint_filter_available");capture(mc,"no-blueprint");
                stage=91;
            } else if(stage==91&&mc.screen instanceof ShipyardScreen screen){
                click(screen,"显示全部");stage=10;delay=4;
            } else if(stage==10&&mc.screen instanceof ShipyardScreen screen){
                require(screen.getMenu().visibleShips().size()==10,"no_blueprint_all_sizes_query");select(screen,"u47");stage=11;delay=4;
            } else if(stage==11&&mc.screen instanceof ShipyardScreen screen){
                require(id("u47").equals(screen.getMenu().browse.selected),"unlicensed_ship_can_be_previewed");require(!button(screen,"建造").active,"unlicensed_query_cannot_build");capture(mc,"no-blueprint-query");stage=111;delay=2;
            } else if(stage==111&&mc.screen instanceof ShipyardScreen screen){
                mc.getSingleplayerServer().execute(()->{
                    var yard=(ShipyardBlockEntity)mc.getSingleplayerServer().overworld().getBlockEntity(POS);
                    var blueprint=BlueprintRules.create(BlueprintType.AIRCRAFT_CARRIER,BlueprintType.BATTLESHIP,BlueprintType.BATTLECRUISER);blueprint.setCount(8);yard.insert(0,blueprint,false);
                    yard.fluid(null).fill(new FluidStack(ModFluids.FUEL_OIL.get(),32000),FluidAction.EXECUTE);yard.start(id("vittorio_veneto"),null);longNameReady=true;
                });select(screen,"saratoga");stage=12;delay=20;
            } else if(stage==12&&longNameReady&&mc.screen instanceof ShipyardScreen screen&&screen.getMenu().shipyard.job()!=null
                    &&screen.getMenu().shipyard.job().recipe.ship().equals(id("vittorio_veneto"))){
                require(screen.getMenu().browse.selected.equals(id("saratoga")),"cross_size_query_does_not_change_task");capture(mc,"long-name");stage=121;delay=2;
            } else if(stage==121&&mc.screen instanceof ShipyardScreen screen){
                click(screen,"取消建造");stage=13;delay=4;
            } else if(stage==13&&mc.screen instanceof ShipyardScreen screen){
                click(screen,"确认取消");stage=14;delay=12;
            } else if(stage==14&&mc.screen instanceof ShipyardScreen screen&&screen.getMenu().shipyard.job()==null){
                require(screen.getMenu().shipyard.fuel().getAmount()==32000&&screen.getMenu().shipyard.stack(0).getCount()==8,"long_task_refunded");
                click(screen,"建造");stage=15;delay=12;
            } else if(stage==15&&mc.screen instanceof ShipyardScreen screen&&screen.getMenu().shipyard.job()!=null){
                require(screen.getMenu().shipyard.job().recipe.ship().equals(id("saratoga"))&&screen.getMenu().shipyard.fuel().getAmount()==27000,"build_button_starts_selected_ship");
                require(List.of("running","cancel-confirmation","output-pending","no-blueprint","no-blueprint-query","long-name").stream().allMatch(name->Files.isRegularFile(output.resolve("screenshots/shipyard-"+name+".png"))),"six_actual_client_screenshots");
                click(screen,"搜索与筛选");stage=16;delay=4;
            } else if(stage==16&&mc.screen instanceof ShipyardScreen screen){
                checkClassCoupling(mc);
                require(screen.getFocused() instanceof EditBox,"search_focuses_input");
                screen.keyPressed(GLFW.GLFW_KEY_E,0,0);screen.charTyped('E',0);
                require(mc.screen==screen&&input(screen).getValue().equals("E"),"inventory_key_types_without_closing_menu");
                click(screen,"×");focusInput(screen);type(screen,"u47");click(screen,"小型");click(screen,"G国");
                require(screen.getMenu().visibleShips().size()==10&&screen.getMenu().browse.selected.equals(id("saratoga")),"draft_does_not_change_applied_browse");
                stage=17;delay=4;
            } else if(stage==17&&mc.screen instanceof ShipyardScreen screen){
                capture(mc,"filter-dialog");stage=171;
            } else if(stage==171&&mc.screen instanceof ShipyardScreen screen){
                UiTooltipCapture.request(screen,output,"shipyard-filter-summary.png",(int)((screen.getGuiLeft()+192-160)*scale(mc)),(int)((screen.getGuiTop()+126-105+50)*scale(mc)));stage=172;
            } else if(stage==172&&mc.screen instanceof ShipyardScreen screen){
                var text=String.join(" ",UiTooltipCapture.TOOLTIP_TEXT.getOrDefault("shipyard-filter-summary.png",List.of()));
                require(List.of("当前筛选","全部舰船","小型","潜艇","G国","u47").stream().allMatch(text::contains),"live_filter_summary_includes_scope_and_all_draft_conditions");
                click(screen,"应用");stage=18;delay=4;
            } else if(stage==18&&mc.screen instanceof ShipyardScreen screen){
                require(screen.getFocused()==button(screen,"搜索与筛选"),"apply_returns_focus_to_search_entry");
                var menu=screen.getMenu();require(menu.browse.filter.active()&&menu.visibleShips().size()==1&&menu.visibleShips().getFirst().id().equals(id("u47")),"text_size_nation_filter_applies");
                require(menu.browse.filter.types().contains(com.lex0v0.piranport.domain.ship.ShipType.SUBMARINE),"ship_class_selection_applies_through_network");
                require(menu.browse.selected==null&&menu.shipyard.job().recipe.ship().equals(id("saratoga")),"hidden_preview_clears_but_task_keeps_running");capture(mc,"filter-active");
                stage=181;
            } else if(stage==181&&mc.screen instanceof ShipyardScreen screen){
                click(screen,"显示全部");require(screen.getMenu().visibleShips().isEmpty()&&screen.getMenu().browse.filter.active(),"search_respects_show_all_scope");
                click(screen,"搜索与筛选");stage=19;delay=3;
            } else if(stage==19&&mc.screen instanceof ShipyardScreen screen){
                click(screen,"重置");screen.keyPressed(GLFW.GLFW_KEY_ESCAPE,0,0);
                require(screen.getMenu().browse.filter.active(),"escape_discards_reset_draft");rightClear(screen);
                require(!screen.getMenu().browse.filter.active()&&!screen.getMenu().browse.all&&screen.getMenu().visibleShips().size()==4,"right_clear_preserves_permission_scope");
                click(screen,"显示全部");click(screen,"搜索与筛选");focusInput(screen);type(screen,"列克");
                click(screen,"大型");click(screen,"中型");click(screen,"U国");click(screen,"E国");click(screen,"应用");
                require(screen.getMenu().browse.filter.types().size()==6&&screen.getMenu().browse.filter.nationalities().size()==2&&screen.getMenu().visibleShips().size()==1,"chinese_input_and_multi_select");
                rightClear(screen);require(screen.getMenu().browse.all&&screen.getMenu().visibleShips().size()==10,"right_clear_preserves_all_scope");
                stage=20;delay=4;
            } else if(stage==20&&mc.screen instanceof ShipyardScreen screen){
                capture(mc,"filter-cleared");stage=201;
            } else if(stage==201&&mc.screen instanceof ShipyardScreen screen){
                Language.inject(ClientLanguage.loadFrom(mc.getResourceManager(),List.of("en_us"),false));click(screen,Component.translatable("gui.piranport.shipyard.search").getString());
                require(screen.children().stream().filter(child->child instanceof Button b&&b.visible&&b.active).map(child->(Button)child)
                        .allMatch(b->mc.font.width(b.getMessage())<=b.getWidth()-4),"english_abbreviations_fit_all_buttons");
                var codes=List.of("BB","BC","CV","CA","CVL","CL","BM","DD","SS");
                for(int i=0;i<codes.size();i++){
                    var type=button(screen,codes.get(i));var nation=button(screen,i==8?"Other":List.of("C","E","F","G","I","J","S","U").get(i));
                    require(type.getX()==nation.getX()&&type.getWidth()==nation.getWidth()&&type.getY()==button(screen,"BB").getY(),"class_nation_column_"+i);
                }
                click(screen,"BB");click(screen,"BC");click(screen,"CV");
                focusInput(screen);type(screen,"HoOd");stage=21;delay=4;
            } else if(stage==21&&mc.screen instanceof ShipyardScreen screen){
                capture(mc,"filter-english");stage=211;
            } else if(stage==211&&mc.screen instanceof ShipyardScreen screen){
                var carrier=button(screen,"CVL");
                UiTooltipCapture.request(screen,output,"shipyard-class-full-name.png",(int)((carrier.getX()+5)*scale(mc)),(int)((carrier.getY()+5)*scale(mc)));stage=212;
            } else if(stage==212&&mc.screen instanceof ShipyardScreen screen){
                require(UiTooltipCapture.TOOLTIP_TEXT.getOrDefault("shipyard-class-full-name.png",List.of()).contains("Light Aircraft Carrier"),"long_class_name_available_in_actual_tooltip");
                screen.keyPressed(GLFW.GLFW_KEY_ENTER,0,0);
                require(screen.getMenu().browse.filter.types().equals(java.util.Set.of(ShipType.BATTLESHIP,ShipType.BATTLECRUISER,ShipType.AIRCRAFT_CARRIER)),"manual_class_group_applies_without_extra_size_restriction");
                require(screen.getMenu().visibleShips().size()==1&&screen.getMenu().visibleShips().getFirst().id().equals(id("hood")),"enter_applies_english_case_insensitive_search");
                rightClear(screen);Language.inject(ClientLanguage.loadFrom(mc.getResourceManager(),List.of("en_us","zh_cn"),false));
                CHECKS.put("success",true);Files.writeString(output.resolve("summary.json"),new GsonBuilder().setPrettyPrinting().create().toJson(CHECKS));
                ModLog.evidence(ModLog.Evidence.SHIPYARD_UI, new GsonBuilder().create().toJson(CHECKS));stage=100;mc.stop();
            }
        } catch(Throwable error){
            ModLog.failure(ModLog.Failure.SHIPYARD_UI, error);CHECKS.put("success",false);CHECKS.put("error",error.toString());
            try {if(output!=null)Files.writeString(output.resolve("summary.json"),new GsonBuilder().setPrettyPrinting().create().toJson(CHECKS));}catch(Exception reportError){ModLog.failure(ModLog.Failure.REPORT, reportError);}
            stage=100;mc.stop();
        }
    }

    private static void select(ShipyardScreen screen,String ship){
        var menu=screen.getMenu();var target=id(ship);int index=0;while(index<menu.visibleShips().size()&&!menu.visibleShips().get(index).id().equals(target))index++;
        if(index==menu.visibleShips().size())throw new IllegalStateException("Missing query target "+ship);
        menu.browse.scroll=Math.max(0,index-3);var mc=Minecraft.getInstance();float s=scale(mc);
        int left=(int)((mc.getWindow().getGuiScaledWidth()/s-348)/2),top=Math.max(25,(int)((mc.getWindow().getGuiScaledHeight()/s-252)/2));
        screen.mouseClicked((left+100)*s,(top+38+(index-menu.browse.scroll)*24+8)*s,0);
    }

    private static float scale(Minecraft mc){return Math.max(.25f,Math.min(1,Math.min((mc.getWindow().getGuiScaledWidth()-12)/348f,(mc.getWindow().getGuiScaledHeight()-12)/280f)));}
    // 直接操作真实弹窗控件，检查分组双向联动及部分选择不会丢失匹配结果。
    private static void checkClassCoupling(Minecraft mc){
        var d=new ShipyardFilterDialog(mc.font,192,126,ShipyardFilter.NONE,true,f->0,f->{},()->{});
        for(var size:ShipSize.values()){
            var group=java.util.EnumSet.noneOf(ShipType.class);for(var t:ShipType.values())if(t.size()==size)group.add(t);
            press(d,Component.translatable(size.translationKey()).getString());
            require(d.value().types().equals(group),"size_selects_all_classes_"+size.path());
            var first=group.iterator().next();press(d,Component.translatable("gui.piranport.shipyard.type."+first.path()).getString());
            require(d.value().types().size()==group.size()-1&&!d.summary().getString().contains(Component.translatable(size.translationKey()).getString()),"partial_classes_clear_size_"+size.path());
            press(d,Component.translatable("gui.piranport.shipyard.type."+first.path()).getString());
            require(d.value().types().equals(group)&&d.summary().getString().contains(Component.translatable(size.translationKey()).getString()),"all_classes_select_size_"+size.path());
            press(d,Component.translatable(size.translationKey()).getString());
            require(!d.value().active(),"size_deselects_its_classes_"+size.path());
        }
        press(d,Component.translatable(ShipSize.LARGE.translationKey()).getString());
        press(d,Component.translatable("gui.piranport.shipyard.type.heavy_cruiser").getString());
        require(d.value().types().size()==4&&d.value().matches(ShipDefinitions.byId(id("quincy"))),"partial_other_group_remains_in_results");
        var reopened=new ShipyardFilterDialog(mc.font,192,126,d.value(),true,f->0,f->{},()->{});
        require(reopened.value().equals(d.value())&&reopened.summary().equals(d.summary()),"coupled_selection_restores_from_browse");
        press(d,Component.translatable(ShipSize.LARGE.translationKey()).getString());
        require(d.value().types().equals(java.util.Set.of(ShipType.HEAVY_CRUISER)),"deselect_group_keeps_other_partial_group");
    }
    private static void press(ShipyardFilterDialog d,String text){d.widgets().stream().filter(w->w instanceof Button&&w.getMessage().getString().equals(text)).map(w->(Button)w).findFirst().orElseThrow().onPress();}
    private static Button button(ShipyardScreen screen,String label){return screen.children().stream().filter(child->child instanceof Button b&&b.visible&&b.getMessage().getString().equals(label)).map(child->(Button)child).toList().getLast();}
    private static void click(ShipyardScreen screen,String label){var button=button(screen,label);float s=scale(Minecraft.getInstance());screen.mouseClicked((button.getX()+button.getWidth()/2.)*s,(button.getY()+button.getHeight()/2.)*s,0);}
    private static void rightClear(ShipyardScreen screen){var b=button(screen,Component.translatable("gui.piranport.shipyard.search").getString());float s=scale(Minecraft.getInstance());screen.mouseClicked((b.getX()+6)*s,(b.getY()+6)*s,1);}
    private static EditBox input(ShipyardScreen screen){return screen.children().stream().filter(child->child instanceof EditBox).map(child->(EditBox)child).findFirst().orElseThrow();}
    private static void focusInput(ShipyardScreen screen){var input=input(screen);float s=scale(Minecraft.getInstance());screen.mouseClicked((input.getX()+3)*s,(input.getY()+3)*s,0);}
    private static void type(ShipyardScreen screen,String value){value.chars().forEach(character->screen.charTyped((char)character,0));}
    private static void capture(Minecraft mc,String name){
        UiTooltipCapture.requestScreen(mc.screen,output,"shipyard-"+name+".png");
        if(name.equals("no-blueprint-query")||name.equals("long-name")){
            float z=scale(mc);int left=(int)((mc.getWindow().getGuiScaledWidth()/z-348)/2),top=Math.max(25,(int)((mc.getWindow().getGuiScaledHeight()/z-252)/2));
            UiTooltipCapture.request(mc.screen,output,"shipyard-"+name+"-classification.png",(int)((left+38)*z),(int)((top+128)*z));
        }
    }
}
