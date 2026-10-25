package com.lex0v0.piranport;

import com.lex0v0.piranport.blockentity.AmmunitionFactoryBlockEntity;
import com.lex0v0.piranport.blockentity.ammunitionfactory.AmmunitionJob;
import com.lex0v0.piranport.blockentity.manufacturing.*;
import com.lex0v0.piranport.domain.ammunition.*;
import com.lex0v0.piranport.menu.AmmunitionFactoryMenu;
import com.lex0v0.piranport.network.payload.*;
import com.lex0v0.piranport.recipe.AmmunitionFactoryRecipe;
import com.lex0v0.piranport.registry.*;
import com.lex0v0.piranport.text.ManufacturingText;
import io.netty.buffer.Unpooled;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.items.ItemStackHandler;

// 通过真实库存、配方、菜单、掉落和能力验证生产事务与材料守恒。
@GameTestHolder(PiranPort.MODID)
@PrefixGameTestTemplate(false)
public final class AmmunitionFactoryGameTests {
    private static final BlockPos POS=new BlockPos(2,2,2);
    private static final AmmunitionType HE=AmmunitionType.HIGH_EXPLOSIVE_SHELL;
    private static ResourceLocation id(String name){return ResourceLocation.fromNamespaceAndPath(PiranPort.MODID,name);}
    private static AmmunitionFactoryBlockEntity factory(GameTestHelper h){h.setBlock(POS,Blocks.AIR);h.setBlock(POS,ModBlocks.AMMUNITION_FACTORY.get());return(AmmunitionFactoryBlockEntity)h.getBlockEntity(POS);}
    private static Player player(GameTestHelper h){var p=h.makeMockPlayer(GameType.SURVIVAL);var c=h.absolutePos(POS).getCenter();p.setPos(c.x,c.y,c.z);return p;}
    private static void add(AmmunitionFactoryBlockEntity f,ItemStack stack){for(int i=0;i<8&&!stack.isEmpty();i++)stack=f.insert(i,stack,false);if(!stack.isEmpty())throw new IllegalStateException("fixture overflow");}
    private static void supply(AmmunitionFactoryBlockEntity f,AmmunitionType type,int runs){var r=f.recipe(type.id());if(r.copper()>0)add(f,new ItemStack(ModItems.COPPER_NUGGET.get(),r.copper()*runs));if(r.steel()>0)add(f,new ItemStack(ModItems.STEEL_NUGGET.get(),r.steel()*runs));if(r.aluminum()>0)add(f,new ItemStack(ModItems.ALUMINUM_NUGGET.get(),r.aluminum()*runs));if(r.gunpowder()>0)add(f,new ItemStack(Items.GUNPOWDER,r.gunpowder()*runs));}
    private static void advance(AmmunitionFactoryBlockEntity f,int ticks){for(int i=0;i<ticks;i++)AmmunitionFactoryBlockEntity.tick(f.getLevel(),f.getBlockPos(),f.getBlockState(),f);}
    private static int count(AmmunitionFactoryBlockEntity f,Item item){int n=0;for(int i=0;i<16;i++)if(f.stack(i).is(item))n+=f.stack(i).getCount();return n;}
    private static CompoundTag saved(AmmunitionFactoryBlockEntity f){return f.saveWithoutMetadata(f.getLevel().registryAccess());}
    private static void contents(AmmunitionFactoryBlockEntity f,int slot,ItemStack stack){var t=saved(f);var inv=new ItemStackHandler(16);inv.deserializeNBT(f.getLevel().registryAccess(),t.getCompound("Inventory"));inv.setStackInSlot(slot,stack);t.put("Inventory",inv.serializeNBT(f.getLevel().registryAccess()));f.loadWithComponents(t,f.getLevel().registryAccess());}
    private static void fullOutput(AmmunitionFactoryBlockEntity f){for(int i=8;i<16;i++)contents(f,i,new ItemStack(ModItems.HIGH_EXPLOSIVE_SHELL.get(),64));}
    private static void start(GameTestHelper h,AmmunitionFactoryBlockEntity f,AmmunitionType type,int pieces){h.assertTrue(f.start(type.id(),pieces)==AmmunitionFactoryBlockEntity.StartResult.STARTED,"plan starts");}

    @GameTest(template="empty")
    public static void eightConfirmedRecipesAndActualOutputs(GameTestHelper h){
        int[][] values={{4,4,4,0,3,2},{4,4,8,0,2,3},{4,4,4,4,3,3},{4,0,4,0,1,2},{1,2,4,0,2,2},{1,3,4,2,2,3},{1,6,9,6,3,5},{1,4,5,6,3,4}};
        for(var type:AmmunitionType.values()){
            var f=factory(h);var r=f.recipe(type.id());int[] v=values[type.ordinal()];
            h.assertTrue(Arrays.equals(v,new int[]{r.count(),r.copper(),r.steel(),r.aluminum(),r.gunpowder(),r.seconds()}),"confirmed recipe "+type);
            supply(f,type,1);start(h,f,type,r.count());advance(f,r.ticks());h.assertTrue(f.job()==null&&count(f,ModItems.ammunition(type))==r.count(),"exact batch output "+type);
            advance(f,150);h.assertTrue(count(f,ModItems.ammunition(type))==r.count(),"no extra plan "+type);
        }h.succeed();
    }
    @GameTest(template="empty")
    public static void copperConversionsAndTags(GameTestHelper h){
        var copper=new ItemStack(ModItems.COPPER_NUGGET.get());h.assertTrue(copper.getMaxStackSize()==64&&AmmunitionMaterials.value(copper,"copper")==1,"copper nugget tag");
        var split=(CraftingRecipe)h.getLevel().getRecipeManager().byKey(id("copper_nugget_from_ingot")).orElseThrow().value();
        var input=CraftingInput.of(1,1,List.of(new ItemStack(Items.COPPER_INGOT)));
        h.assertTrue(split.matches(input,h.getLevel())&&split.assemble(input,h.getLevel().registryAccess()).getCount()==9,"one ingot to nine nuggets");
        var combine=(CraftingRecipe)h.getLevel().getRecipeManager().byKey(id("copper_ingot_from_nuggets")).orElseThrow().value();
        var grid=CraftingInput.of(3,3,java.util.stream.IntStream.range(0,9).mapToObj(i->copper.copy()).toList());
        h.assertTrue(combine.matches(grid,h.getLevel())&&combine.assemble(grid,h.getLevel().registryAccess()).is(Items.COPPER_INGOT),"nine tagged nuggets to vanilla ingot");h.succeed();
    }
    @GameTest(template="empty")
    public static void onlyFourMaterialsAndNoCopperInOtherFactories(GameTestHelper h){
        var f=factory(h);for(Item item:List.of(Items.IRON_INGOT,Items.IRON_NUGGET,ModItems.SALTPETER.get(),ModItems.HIGH_EXPLOSIVE_SHELL.get()))h.assertTrue(!f.accepts(0,new ItemStack(item)),"wrong material rejected");
        for(Item item:List.of(Items.COPPER_BLOCK,Items.COPPER_INGOT,ModItems.COPPER_NUGGET.get(),ModItems.STEEL_BLOCK_ITEM.get(),ModItems.STEEL_INGOT.get(),ModItems.STEEL_NUGGET.get(),ModItems.ALUMINUM_BLOCK_ITEM.get(),ModItems.ALUMINUM_INGOT.get(),ModItems.ALUMINUM_NUGGET.get(),Items.GUNPOWDER))h.assertTrue(f.accepts(0,new ItemStack(item)),"material accepted "+item);
        h.assertTrue(!ManufacturingMaterials.accepts(new ItemStack(Items.COPPER_INGOT))&&!ManufacturingMaterials.accepts(new ItemStack(ModItems.COPPER_NUGGET.get())),"existing manufacturing whitelist preserved");h.succeed();
    }
    @GameTest(template="empty")
    public static void fragmentsBeforeIngotsAndBlocks(GameTestHelper h){
        var f=factory(h);f.insert(0,new ItemStack(Items.COPPER_BLOCK),false);f.insert(1,new ItemStack(Items.COPPER_INGOT),false);f.insert(2,new ItemStack(ModItems.COPPER_NUGGET.get(),4),false);f.insert(3,new ItemStack(ModItems.STEEL_NUGGET.get(),4),false);f.insert(4,new ItemStack(Items.GUNPOWDER,3),false);
        start(h,f,HE,4);h.assertTrue(f.stack(0).getCount()==1&&f.stack(1).getCount()==1&&f.stack(2).isEmpty(),"nuggets consumed before earlier blocks and ingots");h.succeed();
    }
    @GameTest(template="empty")
    public static void partialIngotHasExactChange(GameTestHelper h){
        var f=factory(h);add(f,new ItemStack(Items.COPPER_INGOT));add(f,new ItemStack(ModItems.STEEL_INGOT.get()));add(f,new ItemStack(Items.GUNPOWDER,3));start(h,f,HE,4);
        h.assertTrue(count(f,ModItems.COPPER_NUGGET.get())==5&&count(f,ModItems.STEEL_NUGGET.get())==5&&count(f,Items.COPPER_INGOT)==0,"exact copper and steel change");h.succeed();
    }
    private static void coarseStock(AmmunitionFactoryBlockEntity f){f.insert(0,new ItemStack(Items.COPPER_BLOCK,64),false);f.insert(1,new ItemStack(ModItems.STEEL_BLOCK_ITEM.get(),64),false);for(int i=2;i<7;i++)f.insert(i,new ItemStack(ModItems.ALUMINUM_BLOCK_ITEM.get(),64),false);f.insert(7,new ItemStack(Items.GUNPOWDER,64),false);}
    @GameTest(template="empty")
    public static void changeOverflowUsesAdjacentInventory(GameTestHelper h){
        var f=factory(h);coarseStock(f);h.setBlock(POS.north(),Blocks.CHEST);var chest=(ChestBlockEntity)h.getBlockEntity(POS.north());start(h,f,HE,4);
        h.assertTrue(chest.countItem(Items.COPPER_INGOT)==8&&chest.countItem(ModItems.COPPER_NUGGET.get())==5&&chest.countItem(ModItems.STEEL_INGOT.get())==8&&chest.countItem(ModItems.STEEL_NUGGET.get())==5,"exact remainder sent to neighbor before dropping");h.assertTrue(f.job().inFlight,"change does not stall production");h.succeed();
    }
    @GameTest(template="empty")
    public static void rejectedNeighborDropsChangeWithoutPause(GameTestHelper h){
        var f=factory(h);coarseStock(f);h.setBlock(POS.north(),Blocks.CHEST);var chest=(ChestBlockEntity)h.getBlockEntity(POS.north());for(int i=0;i<chest.getContainerSize();i++)chest.setItem(i,new ItemStack(Items.COBBLESTONE,64));start(h,f,HE,4);
        int dropped=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(f.getBlockPos()).inflate(2)).stream().mapToInt(e->e.getItem().getCount()).sum();
        h.assertTrue(dropped==26&&f.job().inFlight,"unaccepted 77 copper and 77 steel units normalize into 26 dropped items");h.succeed();
    }
    @GameTest(template="empty")
    public static void quantityRoundingLimitsAndZeroCosts(GameTestHelper h){
        var f=factory(h);supply(f,HE,3);h.assertTrue(f.maximum(f.recipe(HE.id()))==12,"complete-cycle material ceiling");
        h.assertTrue(AmmunitionMaterials.normalize(5,4,12)==8&&AmmunitionMaterials.normalize(5,4,4)==4&&AmmunitionMaterials.normalize(0,4,12)==0&&AmmunitionMaterials.normalize(-1,4,12)==0&&AmmunitionMaterials.normalize(Long.MAX_VALUE,4,12)==0,"round up then cap and reject invalid values");
        var r=new AmmunitionFactoryRecipe(HE.id(),4,0,0,0,1,1);h.assertTrue(AmmunitionMaterials.maximum(List.of(new ItemStack(Items.GUNPOWDER,2)),r)==8,"zero metal costs excluded from division");h.succeed();
    }
    @GameTest(template="empty")
    public static void onlyCurrentCyclePaidAndFiniteRepeat(GameTestHelper h){
        var f=factory(h);supply(f,HE,3);start(h,f,HE,12);h.assertTrue(count(f,Items.GUNPOWDER)==6&&count(f,ModItems.COPPER_NUGGET.get())==8,"only first cycle paid");advance(f,40);
        h.assertTrue(f.job().completedRuns==1&&f.job().inFlight&&count(f,Items.GUNPOWDER)==3&&count(f,ModItems.HIGH_EXPLOSIVE_SHELL.get())==4,"one deposit and next payment");advance(f,80);
        h.assertTrue(f.job()==null&&count(f,ModItems.HIGH_EXPLOSIVE_SHELL.get())==12,"finite plan ends exactly");h.succeed();
    }
    @GameTest(template="empty")
    public static void failedStartAndSimulationHaveNoSideEffects(GameTestHelper h){
        var f=factory(h);supply(f,HE,1);fullOutput(f);var before=saved(f);h.assertTrue(f.start(HE.id(),4)==AmmunitionFactoryBlockEntity.StartResult.OUTPUT_FULL,"blocked before charge");
        f.insert(0,new ItemStack(ModItems.COPPER_NUGGET.get()),true);f.extract(0,1,true);f.maximum(f.recipe(HE.id()));f.canFit(f.recipe(HE.id()));
        h.assertTrue(before.equals(saved(f)),"failed start and query do not mutate machine");h.succeed();
    }
    @GameTest(template="empty")
    public static void residualMixedOutputsAndWholeBatchSpace(GameTestHelper h){
        var f=factory(h);supply(f,HE,1);fullOutput(f);contents(f,8,new ItemStack(ModItems.HIGH_EXPLOSIVE_SHELL.get(),61));
        h.assertTrue(!f.canFit(f.recipe(HE.id()))&&f.start(HE.id(),4)==AmmunitionFactoryBlockEntity.StartResult.OUTPUT_FULL,"three free items cannot hold four");
        contents(f,8,new ItemStack(ModItems.HIGH_EXPLOSIVE_SHELL.get(),60));start(h,f,HE,4);advance(f,40);h.assertTrue(f.stack(8).getCount()==64,"residual output merges complete batch");
        f=factory(h);supply(f,HE,1);contents(f,8,new ItemStack(ModItems.ANTI_SHIP_MISSILE.get(),3));start(h,f,HE,4);advance(f,40);
        h.assertTrue(f.stack(8).is(ModItems.ANTI_SHIP_MISSILE.get())&&f.stack(8).getCount()==3&&count(f,ModItems.HIGH_EXPLOSIVE_SHELL.get())==4,"different residual ammo does not prevent start");h.succeed();
    }
    @GameTest(template="empty")
    public static void outputComponentsPreventMerging(GameTestHelper h){
        var f=factory(h);supply(f,HE,1);fullOutput(f);var named=new ItemStack(ModItems.HIGH_EXPLOSIVE_SHELL.get(),60);named.set(DataComponents.CUSTOM_NAME,Component.literal("reserved"));contents(f,8,named);
        h.assertTrue(!f.canFit(f.recipe(HE.id())),"same item with different components is not free space");h.succeed();
    }
    @GameTest(template="empty")
    public static void nextCycleWaitsForMaterialsThenResumes(GameTestHelper h){
        var f=factory(h);supply(f,HE,2);start(h,f,HE,8);for(int i=0;i<8;i++)f.extract(i,64,false);advance(f,40);
        h.assertTrue(f.job()!=null&&!f.job().inFlight&&f.job().completedRuns==1&&f.status().equals("materials"),"missing future material pauses fixed plan");supply(f,HE,1);advance(f,41);
        h.assertTrue(f.job()==null&&count(f,ModItems.HIGH_EXPLOSIVE_SHELL.get())==8,"replenishment resumes remaining cycle");h.succeed();
    }
    @GameTest(template="empty")
    public static void nextCycleWaitsForOutputAndTakingDoesNotUndoCount(GameTestHelper h){
        var f=factory(h);supply(f,HE,2);fullOutput(f);contents(f,8,new ItemStack(ModItems.HIGH_EXPLOSIVE_SHELL.get(),60));start(h,f,HE,8);advance(f,40);
        h.assertTrue(f.job()!=null&&!f.job().inFlight&&f.job().completedRuns==1&&count(f,Items.GUNPOWDER)==3,"full output pauses before next payment");f.extract(8,4,false);advance(f,1);
        h.assertTrue(f.job().completedRuns==1&&f.job().inFlight,"taking output does not reduce completed count");advance(f,40);h.assertTrue(f.job()==null,"free space resumes exactly remaining cycle");h.succeed();
    }
    @GameTest(template="empty")
    public static void gracefulInterruptCompletesOnlyCurrentCycle(GameTestHelper h){
        var f=factory(h);supply(f,HE,3);start(h,f,HE,12);advance(f,13);var id=f.job().id;h.assertTrue(!f.interrupt(UUID.randomUUID())&&f.interrupt(id)&&f.interrupt(id),"stale rejected and repeated interrupt harmless");advance(f,27);
        h.assertTrue(f.job()==null&&count(f,ModItems.HIGH_EXPLOSIVE_SHELL.get())==4&&count(f,Items.GUNPOWDER)==6,"current paid cycle completes and remaining stock stays");h.succeed();
    }
    @GameTest(template="empty")
    public static void waitingPlanInterruptsImmediately(GameTestHelper h){
        var f=factory(h);supply(f,HE,2);start(h,f,HE,8);for(int i=0;i<8;i++)f.extract(i,64,false);advance(f,40);h.assertTrue(f.interrupt(f.job().id)&&f.job()==null,"no in-flight batch stops immediately");h.succeed();
    }
    @GameTest(template="empty")
    public static void completedBatchWaitsForSpaceAndInterruptFinish(GameTestHelper h){
        var f=factory(h);supply(f,HE,2);start(h,f,HE,8);fullOutput(f);advance(f,60);
        h.assertTrue(f.job().finishedBatch()&&f.job().completedRuns==0&&f.status().equals("pending"),"finished batch waits without partial output");
        f.interrupt(f.job().id);f.extract(8,3,false);advance(f,10);h.assertTrue(f.job()!=null&&f.job().elapsed==40,"less than full batch space still waits");
        f.extract(8,1,false);advance(f,1);h.assertTrue(f.job()==null&&f.stack(8).getCount()==64&&count(f,Items.GUNPOWDER)==3,"one deposit finishes interrupted plan without extra charge");h.succeed();
    }
    @GameTest(template="empty")
    public static void facePermissionsStrictPriorityAndStaleReferences(GameTestHelper h){
        var f=factory(h);supply(f,HE,1);contents(f,8,new ItemStack(ModItems.HIGH_EXPLOSIVE_SHELL.get(),1));var side=f.items(Direction.EAST);var bottom=f.items(Direction.DOWN);
        h.assertTrue(f.items(Direction.UP)==null&&side.getSlots()==16&&!bottom.isItemValid(8,new ItemStack(Items.COPPER_INGOT)),"top closed bottom cannot insert");
        h.assertTrue(bottom.extractItem(8,1,true).isEmpty()&&bottom.extractItem(0,1,true).getCount()==1,"bottom also enforces product priority");
        h.assertTrue(side.extractItem(8,1,false).isEmpty()&&side.extractItem(0,1,false).getCount()==1&&!side.extractItem(8,1,false).isEmpty(),"even targeted raw extraction waits for products");
        h.assertTrue(!side.insertItem(0,new ItemStack(ModItems.HIGH_EXPLOSIVE_SHELL.get()),false).isEmpty(),"product insertion rejected");
        h.assertTrue(h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,f.getBlockPos(),Direction.EAST)==null,"no fluid capability");h.setBlock(POS,Blocks.AIR);
        h.assertTrue(side.extractItem(8,1,false).isEmpty()&&!side.insertItem(8,new ItemStack(Items.COPPER_INGOT),false).isEmpty(),"removed capability inert");h.succeed();
    }
    @GameTest(template="empty")
    public static void rawAndProductMenuRulesAndAnomalousInventory(GameTestHelper h){
        var f=factory(h);contents(f,0,new ItemStack(Items.DIAMOND,5));h.assertTrue(f.extract(0,5,false).is(Items.DIAMOND),"restored unsupported items remain extractable");
        f.menuInventory().setStackInSlot(8,new ItemStack(ModItems.HIGH_EXPLOSIVE_SHELL.get()));h.assertTrue(f.stack(8).isEmpty(),"direct output insertion rejected");
        var p=player(h);p.getInventory().setItem(9,new ItemStack(Items.COPPER_INGOT,3));var menu=new AmmunitionFactoryMenu(1,p.getInventory(),f);menu.quickMoveStack(p,16);h.assertTrue(count(f,Items.COPPER_INGOT)==3,"shift insertion to raw cache");h.succeed();
    }
    @GameTest(template="empty")
    public static void saveLoadPreservesPaidBatchAndInterrupt(GameTestHelper h){
        var f=factory(h);supply(f,HE,3);start(h,f,HE,12);advance(f,17);f.interrupt(f.job().id);var id=f.job().id;var t=saved(f);f.loadWithComponents(t,h.getLevel().registryAccess());
        h.assertTrue(f.job().id.equals(id)&&f.job().elapsed==17&&f.job().stopping&&count(f,Items.GUNPOWDER)==6,"snapshot and payment survive load without offline progress");advance(f,23);
        h.assertTrue(f.job()==null&&count(f,ModItems.HIGH_EXPLOSIVE_SHELL.get())==4,"loaded current batch finishes once");h.succeed();
    }
    @GameTest(template="empty")
    public static void storedRecipeSnapshotIndependentOfCurrentRecipe(GameTestHelper h){
        var f=factory(h);supply(f,HE,1);start(h,f,HE,4);advance(f,7);var t=saved(f);var j=t.getCompound("Job");var recipe=j.getCompound("Recipe");recipe.putInt("Count",8);recipe.putInt("Seconds",1);j.put("Recipe",recipe);t.put("Job",j);f.loadWithComponents(t,h.getLevel().registryAccess());
        h.assertTrue(f.recipe(HE.id()).count()==4&&f.job().recipe.count()==8&&f.job().recipe.seconds()==1,"saved recipe is not repriced from current data");advance(f,13);
        h.assertTrue(count(f,ModItems.HIGH_EXPLOSIVE_SHELL.get())==8&&f.job()==null,"snapshot output and time used");h.succeed();
    }
    @GameTest(template="empty")
    public static void matchingToolDropAndLastPlacementOnlyRestoreCaches(GameTestHelper h){
        var f=factory(h);supply(f,HE,2);start(h,f,HE,8);advance(f,5);var p=player(h);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STONE_PICKAXE));
        f.getBlockState().getBlock().playerWillDestroy(h.getLevel(),f.getBlockPos(),f.getBlockState(),p);
        var drops=Block.getDrops(f.getBlockState(),h.getLevel(),f.getBlockPos(),f,p,p.getMainHandItem());var drop=drops.getFirst();
        var component=drop.get(ModDataComponents.AMMUNITION_FACTORY_STORAGE.get());h.assertTrue(component!=null&&!component.copyTag().contains("Job")&&!component.copyTag().contains("Browse")&&f.job()==null,"harvest component has only caches and no refund");
        p.setItemInHand(InteractionHand.MAIN_HAND,drop);var floor=new BlockPos(5,1,5);h.setBlock(floor,Blocks.STONE);var abs=h.absolutePos(floor);
        drop.getItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(abs).add(0,.5,0),Direction.UP,abs,false)));
        var placed=(AmmunitionFactoryBlockEntity)h.getBlockEntity(floor.above());h.assertTrue(p.getMainHandItem().isEmpty()&&placed.job()==null&&count(placed,Items.GUNPOWDER)==3&&placed.remembered().selected==HE,"last block item preserves unspent cache without job");h.succeed();
    }
    @GameTest(template="empty")
    public static void wrongToolAndRepeatedRemovalDoNotRefundOrDuplicate(GameTestHelper h){
        var f=factory(h);supply(f,HE,2);start(h,f,HE,8);var p=player(h);f.getBlockState().getBlock().playerWillDestroy(h.getLevel(),f.getBlockPos(),f.getBlockState(),p);f.discardForRemoval();h.setBlock(POS,Blocks.AIR);
        var entities=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(f.getBlockPos()).inflate(2));
        h.assertTrue(entities.stream().mapToInt(e->e.getItem().getCount()).sum()==11&&entities.stream().noneMatch(e->e.getItem().is(ModItems.AMMUNITION_FACTORY.get()))&&f.job()==null,"only remaining 4 copper,4 steel,3 powder scatter once");h.succeed();
    }
    @GameTest(template="empty")
    public static void independentWindowsLastCloserAndRemoteRejection(GameTestHelper h){
        var f=factory(h);supply(f,HE,3);var aPlayer=player(h);var bPlayer=player(h);var a=new AmmunitionFactoryMenu(1,aPlayer.getInventory(),f);var b=new AmmunitionFactoryMenu(2,bPlayer.getInventory(),f);
        a.browse.selected=AmmunitionType.ARMOR_PIERCING_SHELL;a.removed(aPlayer);h.assertTrue(b.browse.selected==HE,"other open window does not follow");b.browse.total=true;b.removed(bPlayer);
        var reopened=new AmmunitionFactoryMenu(3,aPlayer.getInventory(),f);h.assertTrue(reopened.browse.selected==HE&&reopened.browse.total,"any player reads last closer state");
        aPlayer.setPos(10000,100,10000);var t=new CompoundTag();t.putString("Target",HE.id().toString());t.putInt("Quantity",4);reopened.handleAction(1,t);h.assertTrue(f.job()==null,"remote intent rejected");h.succeed();
    }
    @GameTest(template="empty")
    public static void machineCraftingAndRecipeBounds(GameTestHelper h){
        var recipe=(CraftingRecipe)h.getLevel().getRecipeManager().byKey(id("ammunition_factory")).orElseThrow().value();
        var steel=new ItemStack(ModItems.STEEL_INGOT.get());var aluminum=new ItemStack(ModItems.ALUMINUM_INGOT.get());
        var input=CraftingInput.of(3,3,List.of(steel,new ItemStack(Items.COPPER_INGOT),steel,aluminum,new ItemStack(Items.PISTON),aluminum,steel,new ItemStack(Items.CRAFTING_TABLE),steel));
        h.assertTrue(recipe.matches(input,h.getLevel())&&recipe.assemble(input,h.getLevel().registryAccess()).is(ModItems.AMMUNITION_FACTORY.get()),"confirmed machine crafting recipe");
        h.assertTrue(new ItemStack(ModItems.AMMUNITION_FACTORY.get()).getMaxStackSize()==1&&ModBlocks.AMMUNITION_FACTORY.get().defaultBlockState().getPistonPushReaction()==net.minecraft.world.level.material.PushReaction.BLOCK,"stack limit and immovable machine");
        var data=new AmmunitionFactoryRecipe(HE.id(),1,1,0,0,0,1).save();data.putInt("Count",513);h.assertTrue(AmmunitionFactoryRecipe.load(data)==null,"oversized output rejected");data.putInt("Count",1);data.putInt("Copper",0);h.assertTrue(AmmunitionFactoryRecipe.load(data)==null,"free recipe rejected");data.putInt("Copper",1);data.putInt("Seconds",Integer.MAX_VALUE);h.assertTrue(AmmunitionFactoryRecipe.load(data)==null,"tick overflow rejected");h.succeed();
    }
    @GameTest(template="empty")
    public static void lootQueriesArePureAndCreativeScattersOnce(GameTestHelper h){
        var f=factory(h);supply(f,HE,2);start(h,f,HE,8);advance(f,7);var before=saved(f);
        Block.getDrops(f.getBlockState(),h.getLevel(),f.getBlockPos(),f);Block.getDrops(f.getBlockState(),h.getLevel(),f.getBlockPos(),f);
        h.assertTrue(before.equals(saved(f)),"loot previews do not cancel or move cache");
        var p=h.makeMockPlayer(GameType.CREATIVE);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.NETHERITE_PICKAXE));f.getBlockState().getBlock().playerWillDestroy(h.getLevel(),f.getBlockPos(),f.getBlockState(),p);h.setBlock(POS,Blocks.AIR);f.discardForRemoval();
        var items=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(f.getBlockPos()).inflate(2));h.assertTrue(items.stream().mapToInt(e->e.getItem().getCount()).sum()==11&&items.stream().noneMatch(e->e.getItem().is(ModItems.AMMUNITION_FACTORY.get())),"creative has no machine or refund and cache scattered once");h.succeed();
    }
    @GameTest(template="empty")
    public static void packetRecipeAndInvalidJobValidation(GameTestHelper h){
        var f=factory(h);var buf=new RegistryFriendlyByteBuf(Unpooled.buffer(),h.getLevel().registryAccess());var r=f.recipe(HE.id());new AmmunitionFactoryRecipe.Serializer().streamCodec().encode(buf,r);h.assertTrue(new AmmunitionFactoryRecipe.Serializer().streamCodec().decode(buf).equals(r),"recipe network roundtrip");buf.release();
        var state=new CompoundTag();state.putString("Selected",HE.id().toString());var payload=new AmmunitionFactorySyncPayload(17,state,state);var b=new RegistryFriendlyByteBuf(Unpooled.buffer(),h.getLevel().registryAccess());AmmunitionFactorySyncPayload.STREAM_CODEC.encode(b,payload);h.assertTrue(AmmunitionFactorySyncPayload.STREAM_CODEC.decode(b).equals(payload),"separate machine and browse packet roundtrip");b.release();
        supply(f,HE,1);start(h,f,HE,4);var t=saved(f);var j=t.getCompound("Job");j.putInt("CompletedRuns",j.getInt("TotalRuns"));h.assertTrue(AmmunitionJob.load(j)==null,"impossible completed count rejected");
        h.assertTrue(ManufacturingText.metalParts(4,"锭","粒").equals("4粒")&&ManufacturingText.metalParts(36,"锭","粒").equals("4锭")&&ManufacturingText.metalParts(83,"锭","粒").equals("9锭2粒"),"zero units omitted without block conversion");h.succeed();
    }
}
