package com.lex0v0.piranport.client;

import com.google.gson.GsonBuilder;
import com.lex0v0.piranport.PiranPort;
import com.lex0v0.piranport.blockentity.AmmunitionFactoryBlockEntity;
import com.lex0v0.piranport.client.gui.screens.AmmunitionFactoryScreen;
import com.lex0v0.piranport.domain.ammunition.AmmunitionType;
import com.lex0v0.piranport.logging.ModLog;
import com.lex0v0.piranport.registry.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.lwjgl.glfw.GLFW;

// 独立新世界实际执行菜单网络与渲染，保留每个关键状态的截图。
@EventBusSubscriber(modid=PiranPort.MODID,value=Dist.CLIENT)
public final class AmmunitionFactoryUiAcceptance {
    private static final BlockPos POS=new BlockPos(0,1,0);
    private static final Map<String,Object> CHECKS=new LinkedHashMap<>();
    private static int stage,ticks,delay;
    private static volatile boolean seeded;
    private static Path output;
    private static String tr(String key){return Component.translatable("gui.piranport.ammunition_factory."+key).getString();}
    private static void require(boolean ok,String key){if(!ok)throw new IllegalStateException(key);CHECKS.put(key,true);}
    private static void server(Consumer<AmmunitionFactoryBlockEntity> action){var s=Minecraft.getInstance().getSingleplayerServer();s.execute(()->action.accept((AmmunitionFactoryBlockEntity)s.overworld().getBlockEntity(POS)));}
    private static void stock(AmmunitionFactoryBlockEntity f){
        for(int i=0;i<16;i++)f.extract(i,64,false);
        f.insert(0,new ItemStack(Items.COPPER_INGOT,32),false);f.insert(1,new ItemStack(ModItems.STEEL_INGOT.get(),32),false);f.insert(2,new ItemStack(ModItems.ALUMINUM_INGOT.get(),32),false);f.insert(3,new ItemStack(Items.GUNPOWDER,64),false);
    }
    private static void open(Minecraft mc){var uuid=mc.player.getUUID();var s=mc.getSingleplayerServer();s.execute(()->{var f=(AmmunitionFactoryBlockEntity)s.overworld().getBlockEntity(POS);s.getPlayerList().getPlayer(uuid).openMenu(f,b->{b.writeBlockPos(POS);b.writeNbt(f.remembered().save());});});}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(!Boolean.getBoolean("piranport.acceptance.ammunitionUi")||stage==100)return;
        var mc=Minecraft.getInstance();
        try{
            if(++ticks>3600)throw new IllegalStateException("Ammunition UI timeout stage "+stage);
            if(UiTooltipCapture.pending())return;if(delay>0){delay--;return;}
            if(stage==0&&AcceptanceClientStartup.titleScreenReady(mc)){
                output=Path.of(System.getProperty("piranport.acceptance.ammunitionUiReport"));Files.createDirectories(output);
                language(mc,true);String world="ammunition-ui-"+System.currentTimeMillis();
                mc.createWorldOpenFlows().createFreshLevel(world,new LevelSettings(world,GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(20261026L,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),new TitleScreen());stage=1;
            }else if(stage==1&&mc.player!=null&&mc.level!=null&&mc.getSingleplayerServer()!=null){
                var uuid=mc.player.getUUID();var s=mc.getSingleplayerServer();s.execute(()->{
                    var l=s.overworld();for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)l.setBlock(new BlockPos(x,0,z),Blocks.STONE.defaultBlockState(),3);
                    l.setBlock(POS,ModBlocks.AMMUNITION_FACTORY.get().defaultBlockState(),3);stock((AmmunitionFactoryBlockEntity)l.getBlockEntity(POS));var p=s.getPlayerList().getPlayer(uuid);p.teleportTo(.5,1.1,3.5);p.getInventory().setItem(9,new ItemStack(ModItems.COPPER_NUGGET.get(),9));seeded=true;
                });stage=2;
            }else if(stage==2&&seeded&&mc.level.getBlockEntity(POS) instanceof AmmunitionFactoryBlockEntity){open(mc);stage=3;delay=10;
            }else if(stage==3&&mc.screen instanceof AmmunitionFactoryScreen s){
                require(tr("pieces_unit").equals("枚"),"ammunition_unit_is_round");require(Component.translatable("tooltip.piranport.manufacturing.ammunition_points",2).getString().contains("点/枚"),"manufacturing_tooltip_uses_round");
                require(s.getMenu().slots.size()==52,"sixteen_machine_and_thirty_six_player_slots");require(s.getMenu().slots.get(0).x==261&&s.getMenu().slots.get(0).y==45&&s.getMenu().slots.get(8).y==109,"centered_right_slot_coordinates");
                require(s.getMenu().browse.selected==AmmunitionType.HIGH_EXPLOSIVE_SHELL,"default_high_explosive");require(s.getMenu().factory.maximum(s.getMenu().factory.recipe(AmmunitionType.HIGH_EXPLOSIVE_SHELL.id()))==84,"maximum_from_current_materials");capture(mc,"单次需求");stage=4;
            }else if(stage==4&&mc.screen instanceof AmmunitionFactoryScreen s){
                type(s,"12");at(s,24,136);require(edit(s).getCursorPosition()==1,"centered_input_click_maps_to_correct_digit");s.keyPressed(GLFW.GLFW_KEY_BACKSPACE,0,0);require(edit(s).getValue().equals("2"),"centered_input_retains_native_editing");
                type(s,"5");s.keyPressed(GLFW.GLFW_KEY_ENTER,0,0);stage=5;delay=8;
            }else if(stage==5&&mc.screen instanceof AmmunitionFactoryScreen s){
                require(s.getMenu().browse.quantity==8&&edit(s).getValue().equals("8"),"pieces_round_up_to_batch_through_network");at(s,215,32);require(!s.getMenu().browse.total,"shorter_switch_excludes_old_left_hit_area");at(s,225,32);type(s,"40");s.keyPressed(GLFW.GLFW_KEY_ENTER,0,0);stage=6;delay=8;
            }else if(stage==6&&mc.screen instanceof AmmunitionFactoryScreen s){
                require(s.getMenu().browse.total&&s.getMenu().browse.quantity==40,"toggle_and_quantity_synced");capture(mc,"总需求");stage=7;
            }else if(stage==7&&mc.screen instanceof AmmunitionFactoryScreen s){
                mc.gameMode.handleInventoryMouseClick(s.getMenu().containerId,16,0,ClickType.QUICK_MOVE,mc.player);stage=8;delay=8;
            }else if(stage==8&&mc.screen instanceof AmmunitionFactoryScreen s){
                require(mc.player.getInventory().getItem(9).isEmpty(),"shift_click_actual_network_insert");require(s.getMenu().factory.stack(4).is(ModItems.COPPER_NUGGET.get()),"client_slot_sync_after_insert");click(s,tr("start"));stage=9;delay=8;
            }else if(stage==9&&mc.screen instanceof AmmunitionFactoryScreen s&&s.getMenu().factory.job()!=null){
                require(s.getMenu().factory.job().totalPieces()==40&&s.getMenu().factory.job().totalRuns==10,"piece_input_becomes_finite_cycle_plan");require(s.getMenu().browse.quantity==40,"running_does_not_rewrite_requested_demand");capture(mc,"生产中");stage=10;
            }else if(stage==10&&mc.screen instanceof AmmunitionFactoryScreen s){click(s,tr("interrupt"));stage=11;delay=6;
            }else if(stage==11&&mc.screen instanceof AmmunitionFactoryScreen s&&s.getMenu().factory.job()!=null&&s.getMenu().factory.job().stopping){
                require(!button(s,tr("stopping_button")).active,"interrupt_waits_for_current_batch");capture(mc,"本次完成后停止");stage=12;delay=45;
            }else if(stage==12&&mc.screen instanceof AmmunitionFactoryScreen s&&s.getMenu().factory.job()==null){
                require(s.getMenu().factory.hasProducts(),"interrupt_deposits_finished_current_batch");at(s,78,48);stage=13;delay=6;
            }else if(stage==13&&mc.screen instanceof AmmunitionFactoryScreen s){
                require(s.getMenu().browse.selected==AmmunitionType.STRAIGHT_RUNNING_TORPEDO,"category_changes_recipe_while_outputs_remain");at(s,70,96);stage=14;delay=6;
            }else if(stage==14&&mc.screen instanceof AmmunitionFactoryScreen s){
                require(s.getMenu().browse.selected==AmmunitionType.GUIDED_TORPEDO,"subtype_selection");capture(mc,"制导鱼雷");stage=15;
            }else if(stage==15&&mc.screen instanceof AmmunitionFactoryScreen s){at(s,106,48);stage=16;delay=6;
            }else if(stage==16&&mc.screen instanceof AmmunitionFactoryScreen s){
                require(s.getMenu().browse.selected==AmmunitionType.ANTI_SHIP_MISSILE,"missile_selection");language(mc,false);mc.setScreen(new AmmunitionFactoryScreen(s.getMenu(),mc.player.getInventory(),Component.translatable("block.piranport.ammunition_factory")));stage=17;delay=6;
            }else if(stage==17&&mc.screen instanceof AmmunitionFactoryScreen s){capture(mc,"英文总需求");stage=18;
            }else if(stage==18&&mc.screen instanceof AmmunitionFactoryScreen s){
                UiTooltipCapture.request(s,output,"英文需求详情.png",(int)((s.getGuiLeft()+175)*scale(mc)),(int)((s.getGuiTop()+50)*scale(mc)));stage=19;
            }else if(stage==19&&mc.screen instanceof AmmunitionFactoryScreen s){
                var tooltip=UiTooltipCapture.TOOLTIP_TEXT.getOrDefault("英文需求详情.png",List.of());
                require(tooltip.size()==5&&tooltip.stream().noneMatch(t->t.contains("ingot")||t.contains("金属单位")),"requirements_tooltip_omits_unit_explanation");s.onClose();stage=20;delay=8;
            }else if(stage==20){open(mc);stage=21;delay=8;
            }else if(stage==21&&mc.screen instanceof AmmunitionFactoryScreen s){
                require(s.getMenu().browse.selected==AmmunitionType.ANTI_SHIP_MISSILE&&s.getMenu().browse.total,"last_closed_browse_saved_on_machine");language(mc,true);mc.setScreen(new AmmunitionFactoryScreen(s.getMenu(),mc.player.getInventory(),Component.translatable("block.piranport.ammunition_factory")));at((AmmunitionFactoryScreen)mc.screen,22,48);server(AmmunitionFactoryUiAcceptance::stock);stage=22;delay=8;
            }else if(stage==22&&mc.screen instanceof AmmunitionFactoryScreen s){type(s,"8");s.keyPressed(GLFW.GLFW_KEY_ENTER,0,0);click(s,tr("start"));stage=23;delay=6;
            }else if(stage==23&&mc.screen instanceof AmmunitionFactoryScreen s&&s.getMenu().factory.job()!=null){server(f->{for(int i=0;i<8;i++)f.extract(i,64,false);});stage=24;delay=45;
            }else if(stage==24&&mc.screen instanceof AmmunitionFactoryScreen s&&s.getMenu().factory.status().equals("materials")){
                require(s.getMenu().factory.job().completedRuns==1&&s.getMenu().browse.quantity==8,"missing_material_pause_keeps_total_demand");capture(mc,"等待原料");stage=25;
            }else if(stage==25&&mc.screen instanceof AmmunitionFactoryScreen s){click(s,tr("interrupt"));stage=26;delay=8;
            }else if(stage==26&&mc.screen instanceof AmmunitionFactoryScreen s&&s.getMenu().factory.job()==null){
                require(!s.getMenu().factory.hasProducts()||s.getMenu().factory.stack(8).getCount()==4,"waiting_interrupt_keeps_completed_products");server(f->{stock(f);var t=f.saveWithoutMetadata(f.getLevel().registryAccess());var inv=new ItemStackHandler(16);inv.deserializeNBT(f.getLevel().registryAccess(),t.getCompound("Inventory"));for(int i=8;i<16;i++)inv.setStackInSlot(i,new ItemStack(ModItems.HIGH_EXPLOSIVE_SHELL.get(),i==8?60:64));t.put("Inventory",inv.serializeNBT(f.getLevel().registryAccess()));f.loadWithComponents(t,f.getLevel().registryAccess());});stage=27;delay=8;
            }else if(stage==27&&mc.screen instanceof AmmunitionFactoryScreen s){type(s,"8");s.keyPressed(GLFW.GLFW_KEY_ENTER,0,0);click(s,tr("start"));stage=28;delay=50;
            }else if(stage==28&&mc.screen instanceof AmmunitionFactoryScreen s&&s.getMenu().factory.status().equals("output")){
                require(s.getMenu().factory.job().completedRuns==1&&!s.getMenu().factory.job().inFlight,"output_full_waits_before_next_payment");capture(mc,"产物已满");stage=29;
            }else if(stage==29&&mc.screen instanceof AmmunitionFactoryScreen s){
                mc.gameMode.handleInventoryMouseClick(s.getMenu().containerId,8,0,ClickType.QUICK_MOVE,mc.player);stage=30;delay=8;
            }else if(stage==30&&mc.screen instanceof AmmunitionFactoryScreen s&&s.getMenu().factory.job()!=null&&s.getMenu().factory.job().inFlight){
                require(s.getMenu().factory.job().completedRuns==1,"actual_output_removal_resumes_without_decrementing_completed");stage=31;delay=45;
            }else if(stage==31&&mc.screen instanceof AmmunitionFactoryScreen s&&s.getMenu().factory.job()==null){
                require(s.getMenu().browse.quantity==8,"finished_plan_does_not_change_browse_quantity");CHECKS.put("success",true);Files.writeString(output.resolve("summary.json"),new GsonBuilder().setPrettyPrinting().create().toJson(CHECKS));ModLog.evidence(ModLog.Evidence.AMMUNITION_UI,new GsonBuilder().create().toJson(CHECKS));stage=100;mc.stop();
            }
        }catch(Throwable error){ModLog.failure(ModLog.Failure.AMMUNITION_UI,error);CHECKS.put("success",false);CHECKS.put("error",error.toString());try{if(output!=null)Files.writeString(output.resolve("summary.json"),new GsonBuilder().setPrettyPrinting().create().toJson(CHECKS));}catch(Exception reportError){ModLog.failure(ModLog.Failure.REPORT,reportError);}stage=100;mc.stop();}
    }
    private static void language(Minecraft mc,boolean chinese){Language.inject(ClientLanguage.loadFrom(mc.getResourceManager(),chinese?List.of("en_us","zh_cn"):List.of("en_us"),false));}
    private static float scale(Minecraft mc){return Math.max(.25f,Math.min(1,Math.min((mc.getWindow().getGuiScaledWidth()-12)/348f,(mc.getWindow().getGuiScaledHeight()-12)/252f)));}
    private static Button button(AmmunitionFactoryScreen s,String text){return s.children().stream().filter(c->c instanceof Button b&&b.visible&&b.getMessage().getString().equals(text)).map(c->(Button)c).findFirst().orElseThrow();}
    private static void click(AmmunitionFactoryScreen s,String text){var b=button(s,text);float z=scale(Minecraft.getInstance());s.mouseClicked((b.getX()+b.getWidth()/2.)*z,(b.getY()+b.getHeight()/2.)*z,0);}
    private static EditBox edit(AmmunitionFactoryScreen s){return(EditBox)s.children().stream().filter(c->c instanceof EditBox).findFirst().orElseThrow();}
    private static void type(AmmunitionFactoryScreen s,String value){var box=edit(s);s.setFocused(box);box.setFocused(true);box.setValue(value);}
    private static void at(AmmunitionFactoryScreen s,int x,int y){float z=scale(Minecraft.getInstance());s.mouseClicked((s.getGuiLeft()+x)*z,(s.getGuiTop()+y)*z,0);}
    private static void capture(Minecraft mc,String name){UiTooltipCapture.requestScreen(mc.screen,output,"弹药生产线-"+name+".png");}
}
