package com.lex0v0.piranport.client;

import com.lex0v0.piranport.client.gui.screens.DevelopmentFactoryScreen;

import com.lex0v0.piranport.logging.ModLog;
import com.google.gson.GsonBuilder;
import com.lex0v0.piranport.PiranPort;
import com.lex0v0.piranport.blockentity.DevelopmentFactoryBlockEntity;
import com.lex0v0.piranport.blockentity.developmentfactory.FactoryBrowseState;
import com.lex0v0.piranport.item.blueprint.*;
import com.lex0v0.piranport.registry.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
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

// 只在独立验收客户端新建临时世界，验证实际菜单、输入、网络和画面。
@EventBusSubscriber(modid=PiranPort.MODID,value=Dist.CLIENT)
public final class DevelopmentFactoryUiAcceptance {
    private static final BlockPos POS=new BlockPos(0,1,0);
    private static final Map<String,Object> CHECKS=new LinkedHashMap<>();
    private static int stage,ticks,delay;
    private static volatile boolean seeded;
    private static int classificationIndex;
    private static final String[] CLASSIFICATION_MODELS={"experimental_coilgun","british_twin_mkn5_6in","us_triple_mk16_8in","us_triple_mk6_16in","japanese_sextuple_610mm","g7e_t10_submarine","additional_armor_small","additional_armor_medium","additional_armor_large"};
    private static final String[] CLASSIFICATIONS={"主炮·小型","主炮·中型（轻）","主炮·中型（重）","主炮·大型","鱼雷发射器·通用","鱼雷发射器·潜用","装甲·小型","装甲·中型","装甲·大型"};
    private static Path output;
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath(PiranPort.MODID,path);}
    private static String tr(String key){return Component.translatable("gui.piranport.development_factory."+key).getString();}
    private static void require(boolean ok,String key){if(!ok)throw new IllegalStateException(key);CHECKS.put(key,true);}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(!Boolean.getBoolean("piranport.acceptance.factoryUi")||stage==100)return;
        var mc=Minecraft.getInstance();
        try{
            if(++ticks>3600)throw new IllegalStateException("Factory UI timeout stage "+stage);
            if(UiTooltipCapture.pending())return;
            if(delay>0){delay--;return;}
            if(stage==0&&AcceptanceClientStartup.titleScreenReady(mc)){
                output=Path.of(System.getProperty("piranport.acceptance.factoryUiReport"));Files.createDirectories(output);
                Language.inject(ClientLanguage.loadFrom(mc.getResourceManager(),List.of("en_us","zh_cn"),false));
                String world="factory-ui-"+System.currentTimeMillis();
                mc.createWorldOpenFlows().createFreshLevel(world,new LevelSettings(world,GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(20261024L,false,false),registry->registry.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),new TitleScreen());stage=1;
            }else if(stage==1&&mc.player!=null&&mc.level!=null&&mc.getSingleplayerServer()!=null){
                var uuid=mc.player.getUUID();var server=mc.getSingleplayerServer();server.execute(()->{
                    var level=server.overworld();for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)level.setBlock(new BlockPos(x,0,z),Blocks.STONE.defaultBlockState(),3);
                    level.setBlock(POS,ModBlocks.DEVELOPMENT_FACTORY.get().defaultBlockState(),3);var f=(DevelopmentFactoryBlockEntity)level.getBlockEntity(POS);
                    var b=BlueprintRules.create(BlueprintType.MAIN_GUN,BlueprintType.QUICK_REPAIR,BlueprintType.FIGHTER);b.setCount(4);f.insert(0,b,false);
                    f.insert(2,new ItemStack(ModItems.STEEL_INGOT.get(),64),false);f.insert(3,new ItemStack(ModItems.ALUMINUM_INGOT.get(),64),false);f.insert(4,new ItemStack(ModItems.MAIN_GUN_AMMUNITION.get(),64),false);f.fluid(null).fill(new FluidStack(ModFluids.FUEL_OIL.get(),16000),FluidAction.EXECUTE);
                    f.start(id("us_triple_mk6_16in"),null);for(int i=0;i<100;i++)DevelopmentFactoryBlockEntity.tick(level,POS,f.getBlockState(),f);
                    var browse=new FactoryBrowseState();browse.all=true;browse.selected=id("phantom_fg1");f.remember(browse);
                    server.getPlayerList().getPlayer(uuid).teleportTo(.5,1.1,3.5);seeded=true;
                });stage=2;
            }else if(stage==2&&seeded&&mc.level.getBlockEntity(POS) instanceof DevelopmentFactoryBlockEntity){
                var uuid=mc.player.getUUID();var server=mc.getSingleplayerServer();server.execute(()->{var f=(DevelopmentFactoryBlockEntity)server.overworld().getBlockEntity(POS);server.getPlayerList().getPlayer(uuid).openMenu(f,b->{b.writeBlockPos(POS);b.writeNbt(f.remembered().save());});});stage=3;delay=8;
            }else if(stage==3&&mc.screen instanceof DevelopmentFactoryScreen s){
                require(s.getMenu().factory.job()!=null,"task_sync");require(s.getMenu().browse.selected.equals(id("phantom_fg1")),"browse_independent_of_task");require(!button(s,tr("tab_repair")).active,"repair_locked");require(s.getMenu().slots.get(0).x==249&&s.getMenu().slots.get(1).x==359,"slots_follow_factory_layout");stage=30;delay=10;
            }else if(stage==30&&mc.screen instanceof DevelopmentFactoryScreen s){capture(mc,"running");stage=31;
            }else if(stage==31&&mc.screen instanceof DevelopmentFactoryScreen s){click(s,tr("cancel"));stage=4;delay=4;
            }else if(stage==4&&mc.screen instanceof DevelopmentFactoryScreen s){
                require(s.getFocused()==button(s,tr("return")),"cancel_defaults_to_return");capture(mc,"cancel");stage=41;
            }else if(stage==41&&mc.screen instanceof DevelopmentFactoryScreen s){click(s,tr("confirm_cancel"));stage=5;delay=12;
            }else if(stage==5&&mc.screen instanceof DevelopmentFactoryScreen s&&s.getMenu().factory.job()==null){
                require(s.getMenu().factory.fuel().getAmount()==16000,"cancel_refunds_through_network");click(s,tr("search"));require(s.getFocused() instanceof EditBox,"search_focus");type(s,"discard me");s.keyPressed(GLFW.GLFW_KEY_ESCAPE,0,0);require(!s.getMenu().browse.filter.active(),"escape_discards_filter_draft");click(s,tr("search"));type(s,"ＭＫ．１６");click(s,Component.translatable("item.piranport.main_gun").getString());stage=6;delay=4;
            }else if(stage==6&&mc.screen instanceof DevelopmentFactoryScreen s){capture(mc,"filter");stage=61;
            }else if(stage==61&&mc.screen instanceof DevelopmentFactoryScreen s){click(s,tr("apply"));stage=7;delay=4;
            }else if(stage==7&&mc.screen instanceof DevelopmentFactoryScreen s){
                require(s.getMenu().visibleEntries().size()==1&&s.getMenu().visibleEntries().getFirst().id().equals(id("us_triple_mk16_8in")),"fullwidth_text_and_category_applied");capture(mc,"filtered");stage=71;
            }else if(stage==71&&mc.screen instanceof DevelopmentFactoryScreen s){var b=button(s,tr("search"));s.mouseClicked((b.getX()+6)*scale(mc),(b.getY()+6)*scale(mc),1);require(!s.getMenu().browse.filter.active()&&s.getMenu().browse.all,"right_clear_preserves_scope");select(s,"quick_repair");click(s,tr("build"));stage=8;delay=8;
            }else if(stage==8&&mc.screen instanceof DevelopmentFactoryScreen s&&s.getMenu().factory.job()!=null){
                require(s.getMenu().factory.job().recipe.count()==8,"quick_batch_snapshot");mc.getSingleplayerServer().execute(()->{var f=(DevelopmentFactoryBlockEntity)mc.getSingleplayerServer().overworld().getBlockEntity(POS);for(int i=0;i<100;i++)DevelopmentFactoryBlockEntity.tick(f.getLevel(),POS,f.getBlockState(),f);f.extract(0,64,false);});stage=9;delay=16;
            }else if(stage==9&&mc.screen instanceof DevelopmentFactoryScreen s&&!s.getMenu().factory.stack(1).isEmpty()){
                require(s.getMenu().factory.stack(1).getCount()==8&&s.getMenu().factory.stack(0).isEmpty(),"batch_completed_one_blueprint");require(button(s,tr("build")).active,"pending_output_button_clickable");s.getMenu().browse.selected=null;click(s,tr("build"));stage=90;delay=4;
            }else if(stage==90&&mc.screen instanceof DevelopmentFactoryScreen s){capture(mc,"pending");stage=91;
            }else if(stage==91&&mc.screen instanceof DevelopmentFactoryScreen s){mc.getSingleplayerServer().execute(()->((DevelopmentFactoryBlockEntity)mc.getSingleplayerServer().overworld().getBlockEntity(POS)).extract(1,64,false));stage=10;delay=16;
            }else if(stage==10&&mc.screen instanceof DevelopmentFactoryScreen s&&s.getMenu().factory.stack(1).isEmpty()){
                click(s,tr("all"));require(s.getMenu().visibleEntries().isEmpty(),"empty_no_blueprint_scope");stage=11;delay=4;
            }else if(stage==11&&mc.screen instanceof DevelopmentFactoryScreen s){capture(mc,"empty");stage=111;
            }else if(stage==111&&mc.screen instanceof DevelopmentFactoryScreen s){click(s,tr("all"));select(s,"phantom_fg1");require(!button(s,tr("build")).active,"unlicensed_preview_disabled_build");stage=12;delay=4;
            }else if(stage==12&&mc.screen instanceof DevelopmentFactoryScreen s){capture(mc,"query");stage=121;
            }else if(stage==121&&mc.screen instanceof DevelopmentFactoryScreen s){Language.inject(ClientLanguage.loadFrom(mc.getResourceManager(),List.of("en_us"),false));mc.setScreen(new DevelopmentFactoryScreen(s.getMenu(),mc.player.getInventory(),Component.translatable("block.piranport.development_factory")));stage=13;delay=6;
            }else if(stage==13&&mc.screen instanceof DevelopmentFactoryScreen s){capture(mc,"english-query");stage=131;
            }else if(stage==131&&mc.screen instanceof DevelopmentFactoryScreen s){click(s,tr("search"));stage=14;delay=4;
            }else if(stage==14&&mc.screen instanceof DevelopmentFactoryScreen s){
                capture(mc,"english-filter");stage=141;
            }else if(stage==141&&mc.screen instanceof DevelopmentFactoryScreen s){
                s.keyPressed(GLFW.GLFW_KEY_ESCAPE,0,0);
                Language.inject(ClientLanguage.loadFrom(mc.getResourceManager(),List.of("en_us","zh_cn"),false));
                mc.setScreen(new DevelopmentFactoryScreen(s.getMenu(),mc.player.getInventory(),Component.translatable("block.piranport.development_factory")));
                stage=15;delay=4;
            }else if(stage==15&&mc.screen instanceof DevelopmentFactoryScreen s){
                select(s,CLASSIFICATION_MODELS[classificationIndex]);stage=16;delay=4;
            }else if(stage==16&&mc.screen instanceof DevelopmentFactoryScreen s){
                var definition=com.lex0v0.piranport.domain.equipment.EquipmentDefinitions.find(s.getMenu().browse.selected).orElseThrow();
                var text=com.lex0v0.piranport.text.ClassificationText.equipment(definition);
                require(text.getString().equals(CLASSIFICATIONS[classificationIndex]),"classification_"+CLASSIFICATION_MODELS[classificationIndex]);
                captureHover(mc,s,"classification-"+classificationIndex,false);
                captureHover(mc,s,"classification-"+classificationIndex+"-model",true);
                if(++classificationIndex<CLASSIFICATION_MODELS.length){stage=15;delay=4;}
                else{stage=17;delay=4;}
            }else if(stage==17){
                CHECKS.put("success",true);Files.writeString(output.resolve("summary.json"),new GsonBuilder().setPrettyPrinting().create().toJson(CHECKS));ModLog.evidence(ModLog.Evidence.FACTORY_UI, new GsonBuilder().create().toJson(CHECKS));stage=100;mc.stop();
            }
        }catch(Throwable error){ModLog.failure(ModLog.Failure.FACTORY_UI, error);CHECKS.put("success",false);CHECKS.put("error",error.toString());try{if(output!=null)Files.writeString(output.resolve("summary.json"),new GsonBuilder().setPrettyPrinting().create().toJson(CHECKS));}catch(Exception reportError){ModLog.failure(ModLog.Failure.REPORT, reportError);}stage=100;mc.stop();}
    }
    private static float scale(Minecraft mc){return Math.max(.25f,Math.min(1,Math.min((mc.getWindow().getGuiScaledWidth()-12)/384f,(mc.getWindow().getGuiScaledHeight()-12)/280f)));}
    private static Button button(DevelopmentFactoryScreen s,String text){return s.children().stream().filter(c->c instanceof Button b&&b.visible&&b.getMessage().getString().equals(text)).map(c->(Button)c).toList().getLast();}
    private static void click(DevelopmentFactoryScreen s,String text){var b=button(s,text);float z=scale(Minecraft.getInstance());s.mouseClicked((b.getX()+b.getWidth()/2.)*z,(b.getY()+b.getHeight()/2.)*z,0);}
    private static void type(DevelopmentFactoryScreen s,String text){text.chars().forEach(c->s.charTyped((char)c,0));}
    private static void select(DevelopmentFactoryScreen s,String name){var m=s.getMenu();int index=0;while(index<m.visibleEntries().size()&&!m.visibleEntries().get(index).id().equals(id(name)))index++;if(index==m.visibleEntries().size())throw new IllegalStateException(name);m.browse.scroll=Math.max(0,index-3);var mc=Minecraft.getInstance();float z=scale(mc);int left=(int)((mc.getWindow().getGuiScaledWidth()/z-384)/2),top=Math.max(25,(int)((mc.getWindow().getGuiScaledHeight()/z-252)/2));s.mouseClicked((left+130)*z,(top+38+(index-m.browse.scroll)*24+8)*z,0);}
    private static void capture(Minecraft mc,String name){
        UiTooltipCapture.requestScreen(mc.screen,output,"factory-"+name+".png");
    }
    private static void captureHover(Minecraft mc,DevelopmentFactoryScreen screen,String name,boolean model){
        float z=scale(mc);int left=(int)((mc.getWindow().getGuiScaledWidth()/z-384)/2),top=Math.max(25,(int)((mc.getWindow().getGuiScaledHeight()/z-252)/2));
        int row=0;var entries=screen.getMenu().visibleEntries();for(int i=0;i<entries.size();i++)if(entries.get(i).id().equals(screen.getMenu().browse.selected))row=i-screen.getMenu().browse.scroll;
        UiTooltipCapture.request(screen,output,"factory-"+name+".png",(int)((left+(model?55:150))*z),(int)((top+(model?94:51+row*24))*z));
    }
}
