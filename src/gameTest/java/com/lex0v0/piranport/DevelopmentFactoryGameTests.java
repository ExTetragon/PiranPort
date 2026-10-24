package com.lex0v0.piranport;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.lex0v0.piranport.blockentity.DevelopmentFactoryBlockEntity;
import com.lex0v0.piranport.blockentity.developmentfactory.*;
import com.lex0v0.piranport.blockentity.manufacturing.ManufacturingMaterials;
import com.lex0v0.piranport.domain.equipment.*;
import com.lex0v0.piranport.item.EquipmentItem;
import com.lex0v0.piranport.item.blueprint.*;
import com.lex0v0.piranport.item.equipment.EquipmentRules;
import com.lex0v0.piranport.item.rigging.*;
import com.lex0v0.piranport.menu.DevelopmentFactoryMenu;
import com.lex0v0.piranport.recipe.DevelopmentFactoryRecipe;
import com.lex0v0.piranport.registry.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.*;

// 通过实际设施、菜单、配方和物品能力验证事务，不实现消耗品作战触发。
@GameTestHolder(PiranPort.MODID)
@PrefixGameTestTemplate(false)
public final class DevelopmentFactoryGameTests {
    private static final BlockPos POS=new BlockPos(2,2,2);
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath(PiranPort.MODID,path);}
    private static FluidStack oil(int amount){return new FluidStack(ModFluids.FUEL_OIL.get(),amount);}
    private static DevelopmentFactoryBlockEntity factory(GameTestHelper h){h.setBlock(POS,ModBlocks.DEVELOPMENT_FACTORY.get());return(DevelopmentFactoryBlockEntity)h.getBlockEntity(POS);}
    private static Player player(GameTestHelper h){var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=h.absolutePos(POS);p.setPos(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);return p;}
    private static ItemStack blueprint(BlueprintType...types){return BlueprintRules.create(BlueprintFamily.EQUIPMENT,List.of(types));}
    private static void add(DevelopmentFactoryBlockEntity f,ItemStack stack){for(int i=2;i<14&&!stack.isEmpty();i++)stack=f.insert(i,stack,false);if(!stack.isEmpty())throw new IllegalStateException("fixture cache overflow");}
    private static void supply(DevelopmentFactoryBlockEntity f,String target){var entry=FactoryCatalog.byId(id(target));var r=f.recipe(entry.id());var b=blueprint(BlueprintType.byId(id("equipment/"+entry.category().path())));b.setCount(3);f.insert(0,b,false);add(f,new ItemStack(ModItems.STEEL_INGOT.get(),r.steel()));add(f,new ItemStack(ModItems.ALUMINUM_INGOT.get(),r.aluminum()));add(f,new ItemStack(ModItems.MAIN_GUN_AMMUNITION.get(),r.ammunition()/2));f.fluid(null).fill(oil(r.fuel()),FluidAction.EXECUTE);}
    private static void advance(DevelopmentFactoryBlockEntity f,int ticks){for(int i=0;i<ticks;i++)DevelopmentFactoryBlockEntity.tick(f.getLevel(),f.getBlockPos(),f.getBlockState(),f);}
    private static void start(GameTestHelper h,DevelopmentFactoryBlockEntity f,String target){h.assertTrue(f.start(id(target),null)==DevelopmentFactoryBlockEntity.StartResult.STARTED,"task starts");}

    @GameTest(template="empty")
    public static void consumablesHaveNoEquipmentState(GameTestHelper h){
        for(var item:List.of(ModItems.QUICK_REPAIR.get(),ModItems.DAMAGE_CONTROL.get())){
            var stack=new ItemStack(item);h.assertTrue(!(item instanceof EquipmentItem)&&!stack.isDamageableItem()&&!stack.has(ModDataComponents.EQUIPMENT_IDENTITY)&&!stack.has(DataComponents.MAX_DAMAGE)&&stack.is(ModItemTags.DEVICES)&&stack.is(ModItemTags.EQUIPMENT_CONSUMABLES),"ordinary consumable has classification only");
        }
        h.assertTrue(new ItemStack(ModItems.QUICK_REPAIR.get()).getMaxStackSize()==64&&new ItemStack(ModItems.DAMAGE_CONTROL.get()).getMaxStackSize()==16,"independent stack limits");
        h.assertTrue(!Config.ALLOW_QUICK_REPAIR_FROM_PLAYER_INVENTORY.get()&&!Config.ALLOW_DAMAGE_CONTROL_FROM_PLAYER_INVENTORY.get(),"inventory options default false");
        h.assertTrue(EquipmentDefinitions.ALL.size()==21&&FactoryCatalog.ALL.size()==23,"consumables removed from durable catalogue");h.succeed();
    }
    @GameTest(template="empty")
    public static void coreConsumableStackLimitsAndSimulation(GameTestHelper h){
        var core=RiggingCoreRules.create(id("lexington_cv2"));var handler=RiggingCoreRules.inventory(core,RiggingRegion.EQUIPMENT);
        var quick=new ItemStack(ModItems.QUICK_REPAIR.get(),64);h.assertTrue(handler.insertItem(0,quick,true).isEmpty()&&handler.getStackInSlot(0).isEmpty(),"64 quick repair simulation pure");handler.insertItem(0,quick,false);
        h.assertTrue(handler.insertItem(0,new ItemStack(ModItems.QUICK_REPAIR.get()),false).getCount()==1,"full consumable stack rejects excess");
        handler.setStackInSlot(1,new ItemStack(ModItems.DAMAGE_CONTROL.get(),16));h.assertTrue(handler.getStackInSlot(1).getCount()==16,"16 damage controls fit");
        var armor=EquipmentRules.create(id("additional_armor_small"));handler.setStackInSlot(2,armor);h.assertTrue(!handler.insertItem(2,armor,false).isEmpty(),"ordinary equipment still one per slot");
        h.assertTrue(RiggingStatsCalculator.calculate(core).orElseThrow().equals(RiggingCoreRules.definition(core).orElseThrow().baseStats()),"consumables provide no entry stats");h.succeed();
    }
    @GameTest(template="empty")
    public static void allTwentyThreeRecipesAndOutputKinds(GameTestHelper h){
        var f=factory(h);h.assertTrue(h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipeTypes.DEVELOPMENT_FACTORY.get()).size()==23,"all recipes loaded");
        for(var entry:FactoryCatalog.ALL){var r=f.recipe(entry.id());h.assertTrue(r!=null&&r.knownTarget()&&r.model()==entry.model()&&r.result().getCount()==(entry.category()==EquipmentCategory.QUICK_REPAIR?8:1),"recipe chooses exact identity and count");}
        var r=new DevelopmentFactoryRecipe(id("experimental_coilgun"),true,1,0,0,100,100,301);h.assertTrue(r.knownTarget(),"guidance does not reject zero resource or larger costs/time");
        h.assertTrue(DevelopmentFactoryRecipe.CODEC.codec().parse(JsonOps.INSTANCE,JsonParser.parseString("{\"target\":\"piranport:quick_repair\",\"kind\":\"model\",\"count\":8,\"fuel\":0,\"ammunition\":0,\"steel\":0,\"aluminum\":0,\"seconds\":1}")).result().isEmpty(),"wrong output kind rejected");h.succeed();
    }
    @GameTest(template="empty")
    public static void blockRecipeRejectsFilledBarrel(GameTestHelper h){
        var recipe=h.getLevel().getRecipeManager().byKey(id("development_factory")).orElseThrow().value();
        var inputs=new ArrayList<ItemStack>(List.of(new ItemStack(ModItems.STEEL_INGOT.get()),new ItemStack(ModItems.ALUMINUM_INGOT.get()),new ItemStack(ModItems.STEEL_INGOT.get()),new ItemStack(ModItems.FUEL_BARREL_ITEM.get()),new ItemStack(Items.CRAFTING_TABLE),new ItemStack(Items.PISTON),new ItemStack(ModItems.STEEL_INGOT.get()),new ItemStack(ModItems.ALUMINUM_INGOT.get()),new ItemStack(ModItems.STEEL_INGOT.get())));
        var craft=(net.minecraft.world.item.crafting.CraftingRecipe)recipe;var input=CraftingInput.of(3,3,inputs);h.assertTrue(craft.matches(input,h.getLevel())&&craft.assemble(input,h.getLevel().registryAccess()).is(ModItems.DEVELOPMENT_FACTORY.get()),"empty barrel creates factory");
        inputs.get(3).set(ModDataComponents.FLUID_CONTENT,SimpleFluidContent.copyOf(oil(1)));h.assertTrue(!craft.matches(CraftingInput.of(3,3,inputs),h.getLevel())&&inputs.get(3).get(ModDataComponents.FLUID_CONTENT).getAmount()==1,"filled barrel rejected without draining");h.succeed();
    }
    @GameTest(template="empty")
    public static void emptyBlueprintScopeAndPureQuery(GameTestHelper h){var f=factory(h);var m=new DevelopmentFactoryMenu(1,player(h).getInventory(),f);h.assertTrue(m.visibleEntries().isEmpty(),"default list empty");m.browse.all=true;h.assertTrue(m.visibleEntries().size()==23&&f.start(id("phantom_fg1"),null)==DevelopmentFactoryBlockEntity.StartResult.INVALID_BLUEPRINT,"show all previews without permit");h.assertTrue(!f.accepts(0,BlueprintRules.create(BlueprintType.DESTROYER))&&!f.accepts(0,new ItemStack(ModItems.EQUIPMENT_BLUEPRINT.get())),"entry and blank blueprints refused");h.succeed();}
    @GameTest(template="empty")
    public static void categoryAndCaseInsensitiveSearch(GameTestHelper h){var f=factory(h);var m=new DevelopmentFactoryMenu(1,player(h).getInventory(),f);m.browse.all=true;for(String query:List.of("FG1","ｆｇ．１","鬼怪")){m.browse.filter=new FactoryFilter(query,Set.of(EquipmentCategory.FIGHTER,EquipmentCategory.MAIN_GUN));h.assertTrue(m.visibleEntries().size()==1,"all names and category union search");}f.insert(0,blueprint(BlueprintType.MAIN_GUN),false);m.browse.all=false;h.assertTrue(m.visibleEntries().isEmpty(),"search remains constrained by blueprint");m.browse.filter=FactoryFilter.NONE;h.assertTrue(m.visibleEntries().size()==5,"class permission covers every main gun");h.succeed();}
    @GameTest(template="empty")
    public static void blueprintReservationAndRefill(GameTestHelper h){var f=factory(h);supply(f,"quick_repair");start(h,f,"quick_repair");h.assertTrue(f.extract(0,64,false).getCount()==2&&f.extract(0,1,false).isEmpty(),"last blueprint reserved");h.assertTrue(f.insert(0,blueprint(BlueprintType.QUICK_REPAIR),false).isEmpty()&&!f.insert(0,blueprint(BlueprintType.QUICK_REPAIR,BlueprintType.DAMAGE_CONTROL),false).isEmpty(),"exact permit refill only");var m=new DevelopmentFactoryMenu(1,player(h).getInventory(),f);m.clicked(0,0,ClickType.SWAP,player(h));h.assertTrue(f.stack(0).getCount()==2,"slot swap cannot remove reserved blueprint");h.succeed();}
    @GameTest(template="empty")
    public static void failedStartDoesNotSpendAnything(GameTestHelper h){var f=factory(h);f.insert(0,blueprint(BlueprintType.MAIN_GUN),false);add(f,new ItemStack(ModItems.STEEL_INGOT.get(),64));f.fluid(null).fill(oil(1000),FluidAction.EXECUTE);var before=f.saveWithoutMetadata(h.getLevel().registryAccess());h.assertTrue(f.start(id("experimental_coilgun"),null)==DevelopmentFactoryBlockEntity.StartResult.MISSING_RESOURCES&&before.equals(f.saveWithoutMetadata(h.getLevel().registryAccess())),"failed planning has no mutation");h.succeed();}
    @GameTest(template="empty")
    public static void simultaneousRequestsAndStaleCancel(GameTestHelper h){var f=factory(h);supply(f,"quick_repair");start(h,f,"quick_repair");var before=f.saveWithoutMetadata(h.getLevel().registryAccess());h.assertTrue(f.start(id("quick_repair"),null)==DevelopmentFactoryBlockEntity.StartResult.BUSY&&!f.cancel(UUID.randomUUID(),null)&&before.equals(f.saveWithoutMetadata(h.getLevel().registryAccess())),"duplicate start and stale cancel do not spend");h.succeed();}
    @GameTest(template="empty")
    public static void completesEachExactOutputWithoutRepeat(GameTestHelper h){var f=factory(h);for(var entry:FactoryCatalog.ALL){supply(f,entry.id().getPath());start(h,f,entry.id().getPath());var r=f.job().recipe;advance(f,r.ticks());var output=f.stack(1);h.assertTrue(FactoryCatalog.definition(output).orElseThrow().id().equals(entry.id())&&output.getCount()==r.count()&&f.stack(0).getCount()==2&&f.job()==null,"one exact output and one blueprint consumed");if(entry.model())h.assertTrue(output.getDamageValue()==0&&EquipmentRules.fuel(output).getFluid().isEmpty(),"equipment full durability and aircraft empty");advance(f,20);h.assertTrue(f.job()==null&&f.start(entry.id(),null)==DevelopmentFactoryBlockEntity.StartResult.OUTPUT_PENDING,"no repeat and pending output blocks even matching stack");f.extract(1,64,false);f.extract(0,64,false);}h.succeed();}
    @GameTest(template="empty")
    public static void taskSnapshotPersistenceAndNoOfflineAdvance(GameTestHelper h){var f=factory(h);supply(f,"quick_repair");start(h,f,"quick_repair");advance(f,17);var id=f.job().id;var saved=f.saveWithoutMetadata(h.getLevel().registryAccess());f.loadWithComponents(saved,h.getLevel().registryAccess());h.assertTrue(f.job().elapsed()==17&&f.job().id.equals(id)&&f.job().recipe.count()==8,"elapsed and batch size restored exactly");advance(f,83);h.assertTrue(f.stack(1).getCount()==8,"snapshot finishes at original time");h.succeed();}
    @GameTest(template="empty")
    public static void cancelledMetalsAndOriginalAmmoRefund(GameTestHelper h){var f=factory(h);f.insert(0,blueprint(BlueprintType.QUICK_REPAIR),false);add(f,new ItemStack(ModItems.STEEL_BLOCK_ITEM.get()));add(f,new ItemStack(ModItems.ALUMINUM_BLOCK_ITEM.get()));var ammo=new ItemStack(ModItems.MISSILE.get(),8);ammo.set(DataComponents.CUSTOM_NAME,Component.literal("batch"));add(f,ammo);f.fluid(null).fill(oil(800),FluidAction.EXECUTE);start(h,f,"quick_repair");h.assertTrue(ManufacturingMaterials.available(f.materials(),"steel")==9,"exact metal change returned");f.cancelCurrent(null);h.assertTrue(ManufacturingMaterials.available(f.materials(),"steel")==81&&ManufacturingMaterials.available(f.materials(),"aluminum")==81&&f.materials().stream().anyMatch(s->ItemStack.isSameItemSameComponents(s,ammo)&&s.getCount()==8)&&f.fuel().getAmount()==800&&f.stack(0).getCount()==1,"net metal, original ammo, fuel and blueprint conserved");h.succeed();}
    @GameTest(template="empty")
    public static void fuelRefundOverflowAndNoPartialBatch(GameTestHelper h){var f=factory(h);supply(f,"quick_repair");start(h,f,"quick_repair");advance(f,99);f.fluid(null).fill(oil(16000),FluidAction.EXECUTE);h.assertTrue(f.destroyedFuelOnCancel()==800,"refund overflow known before cancellation");f.cancelCurrent(null);h.assertTrue(f.stack(1).isEmpty()&&f.fuel().getAmount()==16000&&f.stack(0).getCount()==3,"cancel yields no partial output and destroys only excess refund");h.succeed();}
    @GameTest(template="empty")
    public static void survivalDropsCancelledStorage(GameTestHelper h){var f=factory(h);supply(f,"quick_repair");start(h,f,"quick_repair");var drops=Block.getDrops(f.getBlockState(),h.getLevel(),f.getBlockPos(),f);h.assertTrue(drops.size()==1&&drops.getFirst().is(ModItems.DEVELOPMENT_FACTORY.get())&&drops.getFirst().has(ModDataComponents.DEVELOPMENT_FACTORY_STORAGE)&&f.job()==null,"single container drop includes refunded storage");var data=drops.getFirst().get(ModDataComponents.DEVELOPMENT_FACTORY_STORAGE).copyTag();h.assertTrue(!data.contains("Construction")&&data.contains("Inventory")&&data.contains("Fuel"),"portable storage excludes task progress");h.succeed();}
    @GameTest(template="empty")
    public static void creativeClearsFuelAndScattersItems(GameTestHelper h){var f=factory(h);supply(f,"quick_repair");start(h,f,"quick_repair");f.destroyCreative();h.assertTrue(!f.hasCachedContents()&&f.job()==null&&f.fuel().isEmpty(),"creative destruction clears all retained storage");h.succeed();}
    @GameTest(template="empty")
    public static void sidedCapabilitiesAndRemovedReferences(GameTestHelper h){var f=factory(h);var top=f.items(Direction.UP);var sides=f.items(Direction.EAST);var bottom=f.items(Direction.DOWN);h.assertTrue(top.getSlots()==1&&sides.getSlots()==13&&bottom.getSlots()==1&&f.fluid(Direction.UP)==null,"all face layouts");h.assertTrue(!top.insertItem(0,new ItemStack(ModItems.STEEL_INGOT.get()),false).isEmpty()&&!bottom.insertItem(0,blueprint(BlueprintType.MAIN_GUN),false).isEmpty(),"wrong face input rejected");var fluid=f.fluid(Direction.DOWN);h.setBlock(POS,Blocks.AIR);h.assertTrue(fluid.fill(oil(1000),FluidAction.EXECUTE)==0&&top.extractItem(0,1,false).isEmpty(),"removed capabilities inert");h.succeed();}
    @GameTest(template="empty")
    public static void fuelSimulationAndContainerMaximum(GameTestHelper h){var f=factory(h);var fluid=f.fluid(null);h.assertTrue(fluid.fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE)==0&&fluid.fill(oil(20000),FluidAction.SIMULATE)==16000&&f.fuel().isEmpty(),"fuel filter and pure simulation");var barrel=new ItemStack(ModItems.FUEL_BARREL_ITEM.get());barrel.set(ModDataComponents.FLUID_CONTENT,SimpleFluidContent.copyOf(oil(16000)));fluid.fill(oil(15500),FluidAction.EXECUTE);var result=f.transferContainer(barrel,true,player(h));h.assertTrue(result.isSuccess()&&f.fuel().getAmount()==16000&&result.getResult().get(ModDataComponents.FLUID_CONTENT).getAmount()==15500,"partial maximum transfer preserves unused barrel fuel");h.succeed();}
    @GameTest(template="empty")
    public static void browsingIndependentAndLastCloserSaved(GameTestHelper h){var f=factory(h);var p=player(h);var a=new DevelopmentFactoryMenu(1,p.getInventory(),f);var b=new DevelopmentFactoryMenu(2,p.getInventory(),f);a.browse.all=true;a.browse.filter=new FactoryFilter("FG1",Set.of(EquipmentCategory.FIGHTER));a.normalizeBrowse();h.assertTrue(a.visibleEntries().size()==1&&b.visibleEntries().isEmpty(),"windows independent");a.removed(p);h.assertTrue(f.remembered().filter.active(),"closer writes state");b.removed(p);h.assertTrue(!f.remembered().all&&!f.remembered().filter.active(),"last closer wins without player binding");h.succeed();}
    @GameTest(template="empty")
    public static void menuRejectsRemoteMutation(GameTestHelper h){var f=factory(h);supply(f,"quick_repair");var p=player(h);var m=new DevelopmentFactoryMenu(1,p.getInventory(),f);p.setPos(10000,100,10000);var data=new CompoundTag();data.putString("Target","piranport:quick_repair");m.handleAction(1,data);h.assertTrue(f.job()==null&&f.stack(0).getCount()==3,"remote intent ignored");h.succeed();}
    @GameTest(template="empty")
    public static void frozenBatchOutputSurvivesRecipeDifference(GameTestHelper h){var f=factory(h);supply(f,"quick_repair");start(h,f,"quick_repair");var data=f.saveWithoutMetadata(h.getLevel().registryAccess());var task=data.getCompound("Construction");task.putInt("Count",16);task.putInt("Seconds",1);data.put("Construction",task);f.loadWithComponents(data,h.getLevel().registryAccess());h.assertTrue(f.recipe(id("quick_repair")).count()==8&&f.job().recipe.count()==16,"saved output does not reprice from current data pack");advance(f,20);h.assertTrue(f.stack(1).getCount()==16,"snapshot quantity and time used at completion");h.succeed();}
}
