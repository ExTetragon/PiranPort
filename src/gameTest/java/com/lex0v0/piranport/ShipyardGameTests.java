package com.lex0v0.piranport;

import com.lex0v0.piranport.blockentity.ShipyardBlockEntity;
import com.lex0v0.piranport.blockentity.manufacturing.ManufacturingMaterials;
import com.lex0v0.piranport.blockentity.shipyard.ShipyardFilter;
import com.lex0v0.piranport.blockentity.shipyard.ShipyardBrowseState;
import com.lex0v0.piranport.domain.ship.ShipDefinitions;
import com.lex0v0.piranport.domain.ship.ShipNationality;
import com.lex0v0.piranport.domain.ship.ShipSize;
import com.lex0v0.piranport.item.AmmunitionItem;
import com.lex0v0.piranport.item.blueprint.BlueprintFamily;
import com.lex0v0.piranport.item.blueprint.BlueprintRules;
import com.lex0v0.piranport.item.blueprint.BlueprintType;
import com.lex0v0.piranport.item.rigging.RiggingCoreRules;
import com.lex0v0.piranport.item.rigging.RiggingRegion;
import com.lex0v0.piranport.menu.ShipyardMenu;
import com.lex0v0.piranport.recipe.ShipyardRecipe;
import com.lex0v0.piranport.registry.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

// 通过真实方块、菜单和能力检查预留、结算、掉落及资源守恒。
@GameTestHolder(PiranPort.MODID)
@PrefixGameTestTemplate(false)
public final class ShipyardGameTests {
    private static final BlockPos POS = new BlockPos(2, 2, 2);
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(PiranPort.MODID, path); }
    private static FluidStack oil(int amount) { return new FluidStack(ModFluids.FUEL_OIL.get(), amount); }
    private static ShipyardBlockEntity yard(GameTestHelper h) {
        h.setBlock(POS, ModBlocks.SHIPYARD.get());
        return (ShipyardBlockEntity) h.getBlockEntity(POS);
    }
    private static Player player(GameTestHelper h) {
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        Vec3 center = Vec3.atCenterOf(h.absolutePos(POS)); player.setPos(center.x, center.y, center.z); return player;
    }
    private static ItemStack blueprint(BlueprintType... types) { return BlueprintRules.create(BlueprintFamily.SHIP, List.of(types)); }
    private static void supply(GameTestHelper h, ShipyardBlockEntity yard, String ship, int count) {
        var definition = ShipDefinitions.byId(id(ship)); var recipe = yard.recipe(id(ship));
        ItemStack blueprint = blueprint(BlueprintType.byId(definition.type().id())); blueprint.setCount(count);
        h.assertTrue(yard.insert(0, blueprint, false).isEmpty(), "blueprint inserted");
        add(h, yard, new ItemStack(ModItems.STEEL_INGOT.get(), recipe.steel()));
        add(h, yard, new ItemStack(ModItems.ALUMINUM_INGOT.get(), recipe.aluminum()));
        add(h, yard, new ItemStack(ModItems.MAIN_GUN_AMMUNITION.get(), recipe.ammunition() / 2));
        h.assertTrue(yard.fluid(null).fill(oil(recipe.fuel()), FluidAction.EXECUTE) == recipe.fuel(), "fuel inserted");
    }
    private static void add(GameTestHelper h, ShipyardBlockEntity yard, ItemStack input) {
        ItemStack remaining = input.copy();
        for (int i = 2; i < 14; i++) remaining = yard.insert(i, remaining, false);
        h.assertTrue(remaining.isEmpty(), "materials fit");
    }
    private static void start(GameTestHelper h, ShipyardBlockEntity yard, String ship) {
        h.assertTrue(yard.start(id(ship), null) == ShipyardBlockEntity.StartResult.STARTED, "construction started");
    }
    private static void advance(ShipyardBlockEntity yard, int ticks) {
        for (int i = 0; i < ticks; i++) ShipyardBlockEntity.tick(yard.getLevel(), yard.getBlockPos(), yard.getBlockState(), yard);
    }
    private static int count(ShipyardBlockEntity yard, Item item) {
        return yard.materials().stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
    }
    private static void storedSlot(GameTestHelper h, ShipyardBlockEntity yard, int slot, ItemStack stack) {
        CompoundTag data = yard.saveWithoutMetadata(h.getLevel().registryAccess());
        var inventory = new ItemStackHandler(18); inventory.deserializeNBT(h.getLevel().registryAccess(), data.getCompound("Inventory"));
        inventory.setStackInSlot(slot, stack); data.put("Inventory", inventory.serializeNBT(h.getLevel().registryAccess()));
        yard.loadWithComponents(data, h.getLevel().registryAccess());
    }

    @GameTest(template = "empty")
    public static void recipesAndAmmunition(GameTestHelper h) {
        var yard = yard(h);
        h.assertTrue(h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipeTypes.SHIPYARD.get()).size() == 10, "ten recipes");
        int[] costs = {48, 48, 96, 80, 40, 48, 24, 16, 48, 24};
        for (int i = 0; i < ShipDefinitions.ALL.size(); i++) {
            var recipe = yard.recipe(ShipDefinitions.ALL.get(i).id());
            h.assertTrue(recipe != null && recipe.ammunition() == costs[i] && recipe.ammunition() % 4 == 0
                    && recipe.ticks() == recipe.seconds() * 20, "halved per-ship ammunition cost");
        }
        h.assertTrue(AmmunitionItem.points(new ItemStack(ModItems.DEPTH_CHARGE.get())) == 1
                && AmmunitionItem.points(new ItemStack(ModItems.MAIN_GUN_AMMUNITION.get())) == 2
                && AmmunitionItem.points(new ItemStack(ModItems.TORPEDO.get())) == 2
                && AmmunitionItem.points(new ItemStack(ModItems.MISSILE.get())) == 4, "1/2/2/4 points");
        h.assertTrue(new ItemStack(ModItems.TORPEDO.get()).is(ModItemTags.AMMUNITION), "ammunition tag generated"); h.succeed();
    }

    @GameTest(template = "empty")
    public static void emptyBlueprintQueryAndAdmission(GameTestHelper h) {
        var yard = yard(h); var menu = new ShipyardMenu(1, player(h).getInventory(), yard);
        h.assertTrue(menu.visibleShips().isEmpty(), "default list empty");
        menu.browse.all = true; h.assertTrue(menu.visibleShips().size() == 10, "all sizes query without blueprint");
        var combined = blueprint(BlueprintType.BATTLESHIP, BlueprintType.HEAVY_CRUISER, BlueprintType.DESTROYER);
        h.assertTrue(yard.accepts(0, combined) && !yard.accepts(1, combined), "one blueprint slot accepts all sizes");
        h.assertTrue(!yard.accepts(0, new ItemStack(ModItems.SHIP_BLUEPRINT.get())) && !yard.accepts(0, new ItemStack(Items.PAPER))
                && !yard.accepts(0, BlueprintRules.create(BlueprintFamily.EQUIPMENT, List.of(BlueprintType.MAIN_GUN))), "blank, unrelated and equipment rejected");
        h.assertTrue(yard.start(id("hood"), null) == ShipyardBlockEntity.StartResult.INVALID_BLUEPRINT, "query grants no license");
        yard.insert(0, combined, false); menu.browse.all = false;
        h.assertTrue(menu.visibleShips().size() == 5 && menu.visibleShips().getFirst().size().ordinal() == 0
                && menu.visibleShips().getLast().size().ordinal() == 2, "cross-size permission and stable ordering"); h.succeed();
    }

    @GameTest(template = "empty")
    public static void shipNationalitiesAndBilingualSearch(GameTestHelper h) {
        String[] expected={"u","u","i","e","u","g","c","e","c","g"};
        for(int i=0;i<expected.length;i++)h.assertTrue(ShipDefinitions.ALL.get(i).nationality().path().equals(expected[i]),"nationality follows current ship identity");
        var menu=new ShipyardMenu(1,player(h).getInventory(),yard(h));menu.browse.all=true;
        for(String query:List.of("LEX","lex","列克","ＣＶ－２")){
            menu.browse.filter=new ShipyardFilter(query,Set.of(),Set.of());
            h.assertTrue(menu.visibleShips().size()==1&&menu.visibleShips().getFirst().id().equals(id("lexington_cv2")),"bilingual case-insensitive substring search");
        }
        for(String query:List.of("u-47","U47","ｕ４７")){
            menu.browse.filter=new ShipyardFilter(query,Set.of(),Set.of());
            h.assertTrue(menu.visibleShips().size()==1&&menu.visibleShips().getFirst().id().equals(id("u47")),"punctuation and width normalization");
        }
        menu.browse.filter=new ShipyardFilter("托",Set.of(),Set.of());
        h.assertTrue(menu.visibleShips().size()==2,"all substring matches retained");h.succeed();
    }

    @GameTest(template = "empty")
    public static void filtersIntersectAndRespectBlueprintScope(GameTestHelper h) {
        var yard=yard(h);var menu=new ShipyardMenu(1,player(h).getInventory(),yard);
        menu.browse.filter=new ShipyardFilter("",Set.of(ShipSize.LARGE,ShipSize.MEDIUM),Set.of(ShipNationality.UNITED_STATES,ShipNationality.UNITED_KINGDOM));
        h.assertTrue(menu.visibleShips().isEmpty(),"filters do not bypass empty blueprint scope");
        menu.browse.all=true;h.assertTrue(menu.visibleShips().size()==4,"OR within groups, AND between groups");
        yard.insert(0,blueprint(BlueprintType.AIRCRAFT_CARRIER),false);menu.browse.all=false;
        h.assertTrue(menu.visibleShips().size()==2,"license scope precedes filters");
        menu.browse.filter=new ShipyardFilter("hOoD",Set.of(ShipSize.LARGE),Set.of(ShipNationality.UNITED_KINGDOM));
        h.assertTrue(menu.visibleShips().isEmpty(),"matching name cannot bypass blueprint");
        menu.browse.all=true;h.assertTrue(menu.visibleShips().size()==1,"show all queries unlicensed matches");
        h.assertTrue(yard.start(id("hood"),null)==ShipyardBlockEntity.StartResult.INVALID_BLUEPRINT,"query grants no build permission");
        menu.browse.filter=new ShipyardFilter("",Set.of(),Set.of(ShipNationality.JAPAN));
        h.assertTrue(menu.visibleShips().isEmpty(),"empty nationality group has no matches");
        menu.browse.filter=ShipyardFilter.NONE;h.assertTrue(menu.browse.all&&menu.visibleShips().size()==10,"clear leaves show-all unchanged");h.succeed();
    }

    @GameTest(template = "empty")
    public static void filterStatePersistsIndependentlyOfTask(GameTestHelper h) {
        var yard=yard(h);supply(h,yard,"glowworm",2);start(h,yard,"glowworm");var task=yard.job().id;
        var first=player(h);var second=player(h);var a=new ShipyardMenu(1,first.getInventory(),yard);var b=new ShipyardMenu(2,second.getInventory(),yard);
        a.browse.all=true;a.browse.selected=id("glowworm");a.browse.scroll=6;
        a.browse.filter=new ShipyardFilter("  U47  ",Set.of(ShipSize.SMALL),Set.of(ShipNationality.GERMANY));a.normalizeBrowse();
        h.assertTrue(a.browse.selected==null&&a.browse.scroll==0,"hidden preview clears and scroll clamps");
        a.removed(first);h.assertTrue(!b.browse.filter.active(),"other viewer remains independent");
        var saved=yard.saveWithoutMetadata(h.getLevel().registryAccess());yard.loadWithComponents(saved,h.getLevel().registryAccess());
        var reopened=new ShipyardMenu(3,first.getInventory(),yard);
        h.assertTrue(reopened.browse.filter.equals(a.browse.filter)&&reopened.visibleShips().size()==1,"current filter survives machine save and reopen");
        b.browse.all=false;b.browse.filter=ShipyardFilter.NONE;b.removed(second);
        h.assertTrue(!yard.remembered().filter.active()&&!yard.remembered().all&&yard.job().id.equals(task),"last closer wins without altering task");
        var copied=ShipyardBrowseState.load(a.browse.save());
        h.assertTrue(copied.filter.equals(a.browse.filter)&&copied.all,"network browse format retains filters");h.succeed();
    }

    @GameTest(template = "empty")
    public static void atomicInsufficientResources(GameTestHelper h) {
        var yard = yard(h); supply(h,yard,"glowworm",1);
        for(int slot=2;slot<14;slot++) if(yard.stack(slot).is(ModItems.ALUMINUM_INGOT.get()))yard.extract(slot,64,false);
        CompoundTag before=yard.saveWithoutMetadata(h.getLevel().registryAccess());
        h.assertTrue(yard.start(id("glowworm"),null)==ShipyardBlockEntity.StartResult.MISSING_RESOURCES,"reject insufficient aluminum");
        h.assertTrue(before.equals(yard.saveWithoutMetadata(h.getLevel().registryAccess())),"no partial deduction");h.succeed();
    }

    @GameTest(template = "empty")
    public static void singleWorkstationRejectsConcurrentStart(GameTestHelper h) {
        var yard=yard(h);supply(h,yard,"glowworm",1);
        yard.extract(0,1,false);yard.insert(0,blueprint(BlueprintType.BATTLESHIP,BlueprintType.DESTROYER),false);
        start(h,yard,"glowworm");var running=yard.job().id;
        h.assertTrue(yard.start(id("vittorio_veneto"),player(h))==ShipyardBlockEntity.StartResult.BUSY
                &&yard.job().id.equals(running),"a second ship size cannot create another task");
        advance(yard,900);h.assertTrue(yard.job()==null&&yard.stack(0).isEmpty()
                &&RiggingCoreRules.definition(yard.stack(1)).orElseThrow().id().equals(id("glowworm")),"one task and one output");h.succeed();
    }

    @GameTest(template = "empty")
    public static void reserveOneAndRefill(GameTestHelper h) {
        var yard=yard(h);supply(h,yard,"glowworm",8);start(h,yard,"glowworm");
        var top=yard.items(Direction.UP);h.assertTrue(top.extractItem(0,64,true).getCount()==7&&yard.stack(0).getCount()==8,"simulation caps at seven");
        h.assertTrue(top.extractItem(0,64,false).getCount()==7&&top.extractItem(0,1,false).isEmpty(),"last one protected");
        var extra=blueprint(BlueprintType.DESTROYER);extra.setCount(16);
        h.assertTrue(top.insertItem(0,extra,false).isEmpty()&&yard.stack(0).getCount()==17,"matching refill");
        advance(yard,900);h.assertTrue(yard.stack(0).getCount()==16&&yard.job()==null,"completion consumes reserved one");h.succeed();
    }

    @GameTest(template = "empty")
    public static void exactPermissionAndComponentRefill(GameTestHelper h) {
        var yard=yard(h);supply(h,yard,"glowworm",1);yard.extract(0,1,false);
        var original=blueprint(BlueprintType.DESTROYER,BlueprintType.SUBMARINE);yard.insert(0,original,false);start(h,yard,"glowworm");
        h.assertTrue(!yard.insert(0,blueprint(BlueprintType.DESTROYER),false).isEmpty(),"subset rejected");
        h.assertTrue(!yard.insert(0,blueprint(BlueprintType.DESTROYER,BlueprintType.SUBMARINE,BlueprintType.MONITOR),false).isEmpty(),"superset rejected");
        var renamed=original.copy();renamed.set(DataComponents.CUSTOM_NAME,Component.literal("Different"));
        h.assertTrue(!yard.insert(0,renamed,false).isEmpty(),"different metadata not merged");
        h.assertTrue(yard.insert(0,blueprint(BlueprintType.SUBMARINE,BlueprintType.DESTROYER),false).isEmpty(),"canonical permission order");h.succeed();
    }

    @GameTest(template = "empty")
    public static void menuPickupRefillAndSwapProtection(GameTestHelper h) {
        var yard=yard(h);supply(h,yard,"glowworm",8);start(h,yard,"glowworm");var player=player(h);var menu=new ShipyardMenu(1,player.getInventory(),yard);
        menu.clicked(0,0,ClickType.PICKUP,player);h.assertTrue(menu.getCarried().getCount()==7&&yard.stack(0).getCount()==1,"left pickup leaves one");
        menu.clicked(0,0,ClickType.PICKUP,player);h.assertTrue(menu.getCarried().isEmpty()&&yard.stack(0).getCount()==8,"refill works with only reserved one");
        menu.clicked(0,0,ClickType.SWAP,player);h.assertTrue(yard.stack(0).getCount()==8&&player.getInventory().getItem(0).isEmpty(),"number key cannot replace stack");
        menu.clicked(0,1,ClickType.PICKUP,player);h.assertTrue(menu.getCarried().getCount()==4&&yard.stack(0).getCount()==4,"right pickup splits normally");h.succeed();
    }

    @GameTest(template = "empty")
    public static void shiftAndDoubleCollectionReserve(GameTestHelper h) {
        var yard=yard(h);supply(h,yard,"glowworm",8);start(h,yard,"glowworm");var player=player(h);var menu=new ShipyardMenu(1,player.getInventory(),yard);
        menu.quickMoveStack(player,0);h.assertTrue(yard.stack(0).getCount()==1&&player.getInventory().countItem(ModItems.SHIP_BLUEPRINT.get())==7,"shift leaves reserved one");
        var more=blueprint(BlueprintType.DESTROYER);more.setCount(3);yard.insert(0,more,false);
        menu.setCarried(blueprint(BlueprintType.DESTROYER));menu.clicked(14,0,ClickType.PICKUP_ALL,player);
        h.assertTrue(yard.stack(0).getCount()==1,"double collect cannot take reserved one");
        menu.setCarried(ItemStack.EMPTY);menu.clicked(0,1,ClickType.THROW,player);h.assertTrue(yard.stack(0).getCount()==1,"drop cannot take last blueprint");h.succeed();
    }

    @GameTest(template = "empty")
    public static void shiftBlueprintUsesSingleSlot(GameTestHelper h) {
        var yard=yard(h);var player=player(h);var combined=blueprint(BlueprintType.BATTLESHIP,BlueprintType.DESTROYER);combined.setCount(4);player.getInventory().setItem(9,combined);
        var menu=new ShipyardMenu(1,player.getInventory(),yard);menu.quickMoveStack(player,14);
        h.assertTrue(yard.stack(0).getCount()==4&&player.getInventory().getItem(9).isEmpty(),"shift inserts a cross-size blueprint into the only slot");
        player.getInventory().setItem(9,new ItemStack(ModItems.SHIP_BLUEPRINT.get()));menu.quickMoveStack(player,14);
        h.assertTrue(player.getInventory().getItem(9).is(ModItems.SHIP_BLUEPRINT.get()),"invalid blueprint stays in original slot");
        player.getInventory().setItem(9,new ItemStack(ModItems.EQUIPMENT_BLUEPRINT.get()));menu.quickMoveStack(player,14);
        h.assertTrue(player.getInventory().getItem(9).is(ModItems.EQUIPMENT_BLUEPRINT.get()),"wrong blueprint family stays in original slot");h.succeed();
    }

    @GameTest(template = "empty")
    public static void metalChangeAndNuggetPriority(GameTestHelper h) {
        var yard=yard(h);List<ItemStack> source=new ArrayList<>();
        source.add(new ItemStack(ModBlocks.STEEL_BLOCK.get()));source.add(new ItemStack(ModItems.STEEL_INGOT.get()));source.add(new ItemStack(ModItems.STEEL_NUGGET.get(),3));
        var recipe=new ShipyardRecipe(id("glowworm"),0,0,2,0,1);var plan=ManufacturingMaterials.plan(source,recipe);
        h.assertTrue(plan!=null&&plan.remaining().stream().allMatch(ItemStack::isEmpty),"nuggets and ingots consumed before block");
        h.assertTrue(plan.change().stream().filter(s->s.is(ModItems.STEEL_INGOT.get())).mapToInt(ItemStack::getCount).sum()==8
                &&plan.change().stream().filter(s->s.is(ModItems.STEEL_NUGGET.get())).mapToInt(ItemStack::getCount).sum()==3,"exact 75-nugget change");
        h.assertTrue(source.getFirst().getCount()==1,"planning has no source effects");h.succeed();
    }

    @GameTest(template = "empty")
    public static void ammunitionSlotOrderAndCeiling(GameTestHelper h) {
        var recipe=new ShipyardRecipe(id("glowworm"),0,40,0,0,1);
        var source=List.of(new ItemStack(ModItems.DEPTH_CHARGE.get()),new ItemStack(ModItems.MISSILE.get(),12),new ItemStack(ModItems.MAIN_GUN_AMMUNITION.get(),64));
        var plan=ManufacturingMaterials.plan(source,recipe);
        h.assertTrue(plan!=null&&plan.ammunition().get(0).getCount()==1&&plan.ammunition().get(1).getCount()==10
                &&plan.remaining().get(2).getCount()==64,"41 points in slot order, no optimization");
        var rounded=ManufacturingMaterials.plan(List.of(new ItemStack(ModItems.DEPTH_CHARGE.get()),new ItemStack(ModItems.TORPEDO.get(),2)),new ShipyardRecipe(id("glowworm"),0,4,0,0,1));
        h.assertTrue(rounded!=null&&rounded.ammunition().get(1).getCount()==2,"four points round the last two-point item upwards");h.succeed();
    }

    @GameTest(template = "empty")
    public static void completionOnceAndEmptyCore(GameTestHelper h) {
        var yard=yard(h);supply(h,yard,"glowworm",3);start(h,yard,"glowworm");advance(yard,899);
        h.assertTrue(yard.stack(0).getCount()==3&&yard.stack(1).isEmpty(),"blueprint consumed only at completion");advance(yard,1);
        var core=yard.stack(1);h.assertTrue(RiggingCoreRules.definition(core).orElseThrow().id().equals(id("glowworm"))&&RiggingCoreRules.getProficiency(core)==0&&RiggingCoreRules.fuel(core).getFluid().isEmpty(),"correct empty core");
        for(var region:RiggingRegion.values())h.assertTrue(RiggingCoreRules.inventory(core,region).getStackInSlot(0).isEmpty(),"empty region");
        advance(yard,2000);h.assertTrue(yard.stack(0).getCount()==2&&yard.stack(1).getCount()==1&&yard.job()==null,"no automatic repeat");h.succeed();
    }

    @GameTest(template = "empty")
    public static void pendingOutputPreventsPayment(GameTestHelper h) {
        var yard=yard(h);supply(h,yard,"glowworm",2);start(h,yard,"glowworm");advance(yard,900);
        var before=yard.saveWithoutMetadata(h.getLevel().registryAccess());
        h.assertTrue(yard.start(null,null)==ShipyardBlockEntity.StartResult.OUTPUT_PENDING,"pending result precedes selection check");
        h.assertTrue(before.equals(yard.saveWithoutMetadata(h.getLevel().registryAccess())),"no payment or blueprint reservation");h.succeed();
    }

    @GameTest(template = "empty")
    public static void cancelAndStaleConfirmation(GameTestHelper h) {
        var yard=yard(h);supply(h,yard,"glowworm",2);start(h,yard,"glowworm");UUID old=yard.job().id;advance(yard,450);
        h.assertTrue(yard.cancel(old,null)&&yard.stack(0).getCount()==2&&yard.fuel().getAmount()==1000,"cancel refunds fuel and keeps blueprints");
        h.assertTrue(count(yard,ModItems.STEEL_INGOT.get())==8&&count(yard,ModItems.ALUMINUM_INGOT.get())==4&&count(yard,ModItems.MAIN_GUN_AMMUNITION.get())==8,"refund actual net costs");
        start(h,yard,"glowworm");var current=yard.job().id;h.assertTrue(!yard.cancel(old,null)&&yard.job().id.equals(current),"stale confirmation cannot cancel new task");h.succeed();
    }

    @GameTest(template = "empty")
    public static void cancellationPreservesAmmoComponents(GameTestHelper h) {
        var yard=yard(h);supply(h,yard,"glowworm",1);
        for(int slot=2;slot<14;slot++)if(yard.stack(slot).is(ModItems.MAIN_GUN_AMMUNITION.get()))yard.extract(slot,64,false);
        var missile=new ItemStack(ModItems.MISSILE.get(),4);missile.set(DataComponents.CUSTOM_NAME,Component.literal("Batch A"));add(h,yard,missile);
        start(h,yard,"glowworm");var job=yard.job();h.assertTrue(job.ammunition.getFirst().getCount()==4,"four missiles actually paid");yard.cancel(job.id,null);
        h.assertTrue(yard.materials().stream().anyMatch(s->ItemStack.matches(s,missile)),"exact original ammo metadata refunded");h.succeed();
    }

    @GameTest(template = "empty")
    public static void fuelRefundOverflowDestroyed(GameTestHelper h) {
        var yard=yard(h);supply(h,yard,"glowworm",1);start(h,yard,"glowworm");yard.fluid(null).fill(oil(31900),FluidAction.EXECUTE);
        h.assertTrue(yard.destroyedFuelOnCancel()==900,"predict 900 overflow");yard.cancel(yard.job().id,null);
        h.assertTrue(yard.fuel().getAmount()==32000,"refund fills only remaining capacity");h.succeed();
    }

    @GameTest(template = "empty")
    public static void refundOverflowToCurrentOperator(GameTestHelper h) {
        var yard=yard(h);supply(h,yard,"glowworm",1);start(h,yard,"glowworm");
        for(int i=2;i<14;i++)yard.insert(i,new ItemStack(ModItems.TORPEDO.get(),64),false);
        var current=player(h);yard.cancel(yard.job().id,current);
        h.assertTrue(current.getInventory().countItem(ModItems.STEEL_INGOT.get())==8
                &&current.getInventory().countItem(ModItems.ALUMINUM_INGOT.get())==4
                &&current.getInventory().countItem(ModItems.MAIN_GUN_AMMUNITION.get())==8,"refund goes to cancelling player");h.succeed();
    }

    @GameTest(template = "empty")
    public static void saveReloadResumesFrozenTask(GameTestHelper h) {
        var yard=yard(h);supply(h,yard,"glowworm",2);start(h,yard,"glowworm");advance(yard,317);
        var original=yard.job().id;var saved=yard.saveWithFullMetadata(h.getLevel().registryAccess());
        yard.loadWithComponents(saved,h.getLevel().registryAccess());
        h.assertTrue(yard.job().id.equals(original)&&yard.job().elapsed()==317&&yard.job().recipe.seconds()==45,"job snapshot restored");
        advance(yard,583);h.assertTrue(yard.job()==null&&yard.stack(0).getCount()==1&&!yard.stack(1).isEmpty(),"loaded job completes once");h.succeed();
    }

    @GameTest(template = "empty")
    public static void missingTargetPausesButCanRefund(GameTestHelper h) {
        var yard=yard(h);supply(h,yard,"glowworm",1);start(h,yard,"glowworm");advance(yard,30);
        var saved=yard.saveWithoutMetadata(h.getLevel().registryAccess());saved.getCompound("Construction").putString("Ship","removed:ship");yard.loadWithComponents(saved,h.getLevel().registryAccess());
        advance(yard,1000);h.assertTrue(yard.job().elapsed()==30&&yard.stack(1).isEmpty(),"missing definition pauses task");
        h.assertTrue(yard.cancel(yard.job().id,null)&&count(yard,ModItems.STEEL_INGOT.get())==8,"refund path retained");h.succeed();
    }

    @GameTest(template = "empty")
    public static void abnormalOutputWaitsForSpace(GameTestHelper h) {
        var yard=yard(h);supply(h,yard,"glowworm",1);start(h,yard,"glowworm");storedSlot(h,yard,1,new ItemStack(Items.DIAMOND));advance(yard,900);
        h.assertTrue(yard.job()!=null&&yard.job().finished()&&yard.stack(0).getCount()==1,"blocked output does not consume blueprint");
        yard.extract(1,1,false);advance(yard,1);h.assertTrue(yard.job()==null&&RiggingCoreRules.isCore(yard.stack(1)),"complete after output cleared");h.succeed();
    }

    @GameTest(template = "empty")
    public static void breakDropsStorageAfterCancellation(GameTestHelper h) {
        var yard=yard(h);supply(h,yard,"glowworm",2);start(h,yard,"glowworm");advance(yard,100);
        var context=new LootParams.Builder(h.getLevel()).withParameter(LootContextParams.ORIGIN,yard.getBlockPos().getCenter())
                .withParameter(LootContextParams.TOOL,ItemStack.EMPTY).withParameter(LootContextParams.BLOCK_ENTITY,yard)
                .withParameter(LootContextParams.EXPLOSION_RADIUS,64.0f);
        var drops=yard.getBlockState().getDrops(context);
        h.assertTrue(drops.size()==1&&drops.getFirst().has(ModDataComponents.SHIPYARD_STORAGE.get())&&yard.job()==null,"drop contains cancelled storage");
        var data=drops.getFirst().get(ModDataComponents.SHIPYARD_STORAGE.get()).copyTag();
        h.assertTrue(!data.contains("Construction")&&data.getCompound("Fuel").getInt("amount")==1000,"dropped storage has fuel but no task");
        h.setBlock(new BlockPos(4,2,2),ModBlocks.SHIPYARD.get());var restored=(ShipyardBlockEntity)h.getBlockEntity(new BlockPos(4,2,2));restored.applyComponentsFromItemStack(drops.getFirst());
        h.assertTrue(restored.job()==null&&restored.stack(0).getCount()==2&&count(restored,ModItems.STEEL_INGOT.get())==8,"restore cache without task");h.succeed();
    }

    @GameTest(template = "empty")
    public static void lastBlockItemPlacementRestoresCache(GameTestHelper h) {
        var yard=yard(h);supply(h,yard,"glowworm",2);start(h,yard,"glowworm");
        var drop=Block.getDrops(yard.getBlockState(),h.getLevel(),yard.getBlockPos(),yard).getFirst();
        var player=player(h);player.setItemInHand(InteractionHand.MAIN_HAND,drop);var floor=new BlockPos(5,1,5);h.setBlock(floor,Blocks.STONE);
        var abs=h.absolutePos(floor);var hit=new BlockHitResult(Vec3.atCenterOf(abs).add(0,.5,0),Direction.UP,abs,false);
        drop.getItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
        var restored=(ShipyardBlockEntity)h.getBlockEntity(floor.above());
        h.assertTrue(player.getMainHandItem().isEmpty()&&restored.fuel().getAmount()==1000&&restored.stack(0).getCount()==2,"last consumed item restores components before shrinking");h.succeed();
    }

    @GameTest(template = "empty")
    public static void directionalCapabilitiesAndSimulation(GameTestHelper h) {
        var yard=yard(h);var abs=yard.getBlockPos();
        h.assertTrue(h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,abs,Direction.UP)==null,"no top fluid");
        for(Direction side:Direction.values())if(side!=Direction.UP)h.assertTrue(h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,abs,side)!=null,"five fluid faces");
        h.assertTrue(yard.items(Direction.UP).getSlots()==1&&yard.items(Direction.DOWN).getSlots()==1&&yard.items(Direction.NORTH).getSlots()==13,"item face maps");
        var before=yard.saveWithoutMetadata(h.getLevel().registryAccess());
        yard.items(Direction.UP).insertItem(0,blueprint(BlueprintType.DESTROYER),true);yard.fluid(Direction.DOWN).fill(oil(1234),FluidAction.SIMULATE);
        h.assertTrue(before.equals(yard.saveWithoutMetadata(h.getLevel().registryAccess())),"simulation cannot mutate");
        h.assertTrue(yard.fluid(null).fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE)==0,"water rejected");h.succeed();
    }

    @GameTest(template = "empty")
    public static void oldCapabilitiesStopAfterRemoval(GameTestHelper h) {
        var yard=yard(h);var items=yard.items(Direction.UP);var fluids=yard.fluid(null);yard.insert(0,blueprint(BlueprintType.DESTROYER),false);fluids.fill(oil(1000),FluidAction.EXECUTE);
        h.setBlock(POS,Blocks.AIR);
        h.assertTrue(items.extractItem(0,1,false).isEmpty()&&fluids.drain(1000,FluidAction.EXECUTE).isEmpty()&&fluids.fill(oil(1000),FluidAction.EXECUTE)==0,"old references cannot operate removed entity");h.succeed();
    }

    @GameTest(template = "empty")
    public static void containerMaximumAndWholeBucket(GameTestHelper h) {
        var yard=yard(h);var player=player(h);yard.fluid(null).fill(oil(31500),FluidAction.EXECUTE);
        h.assertTrue(!yard.transferContainer(new ItemStack(ModItems.FUEL_OIL_BUCKET.get()),true,player).isSuccess()&&yard.fuel().getAmount()==31500,"bucket cannot lose half bucket");
        var barrel=new ItemStack(ModBlocks.FUEL_BARREL.get());barrel.set(ModDataComponents.FLUID_CONTENT.get(),SimpleFluidContent.copyOf(oil(4000)));
        var result=yard.transferContainer(barrel,true,player);
        h.assertTrue(result.isSuccess()&&yard.fuel().getAmount()==32000&&result.getResult().get(ModDataComponents.FLUID_CONTENT.get()).copy().getAmount()==3500,"partial tank transfers exact maximum");h.succeed();
    }

    @GameTest(template = "empty")
    public static void worldRightClickDirectionFromInitialFullness(GameTestHelper h) {
        var yard=yard(h);var player=player(h);yard.fluid(null).fill(oil(31500),FluidAction.EXECUTE);
        var barrel=new ItemStack(ModBlocks.FUEL_BARREL.get());barrel.set(ModDataComponents.FLUID_CONTENT.get(),SimpleFluidContent.copyOf(oil(4000)));player.setItemInHand(InteractionHand.MAIN_HAND,barrel);
        var hit=new BlockHitResult(Vec3.atCenterOf(yard.getBlockPos()),Direction.UP,yard.getBlockPos(),false);
        yard.getBlockState().useItemOn(player.getMainHandItem(),h.getLevel(),player,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(yard.fuel().getAmount()==32000,"first click only injects");
        yard.getBlockState().useItemOn(player.getMainHandItem(),h.getLevel(),player,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(yard.fuel().getAmount()==19500&&player.getMainHandItem().get(ModDataComponents.FLUID_CONTENT.get()).copy().getAmount()==16000,"second click takes maximum when initially full");h.succeed();
    }

    @GameTest(template = "empty")
    public static void guiContainerCanWithdrawWhenNotFull(GameTestHelper h) {
        var yard=yard(h);yard.fluid(null).fill(oil(2500),FluidAction.EXECUTE);var player=player(h);var menu=new ShipyardMenu(1,player.getInventory(),yard);
        menu.setCarried(new ItemStack(Items.BUCKET));var request=new CompoundTag();request.putBoolean("Inject",false);menu.handleAction(3,request);
        h.assertTrue(menu.getCarried().is(ModItems.FUEL_OIL_BUCKET.get())&&yard.fuel().getAmount()==1500,"explicit GUI right-click drains nonfull tank");h.succeed();
    }

    @GameTest(template = "empty")
    public static void independentWindowsAndLastClosedState(GameTestHelper h) {
        var yard=yard(h);var first=player(h);var second=player(h);var a=new ShipyardMenu(1,first.getInventory(),yard);var b=new ShipyardMenu(2,second.getInventory(),yard);
        a.browse.all=true;a.browse.selected=id("u47");a.browse.scroll=6;a.removed(first);
        h.assertTrue(!b.browse.all&&yard.remembered().selected.equals(id("u47")),"other open window stays put");
        b.browse.all=true;b.browse.selected=id("hood");b.removed(second);
        var reopened=new ShipyardMenu(3,first.getInventory(),yard);
        h.assertTrue(reopened.browse.selected.equals(id("hood")),"last closer wins without player binding");
        supply(h,yard,"glowworm",1);var observer=h.makeMockPlayer(GameType.SPECTATOR);var center=yard.getBlockPos().getCenter();observer.setPos(center.x,center.y,center.z);
        var readonly=new ShipyardMenu(4,observer.getInventory(),yard);var request=new CompoundTag();request.putString("Ship","piranport:glowworm");readonly.handleAction(1,request);
        h.assertTrue(yard.job()==null&&yard.fuel().getAmount()==1000&&yard.stack(0).getCount()==1,"spectator custom payload cannot mutate construction");h.succeed();
    }

    @GameTest(template = "empty")
    public static void commonTagsAcceptForeignMaterials(GameTestHelper h) {
        java.util.Map<TagKey<Item>,List<Holder<Item>>> original=new java.util.HashMap<>();
        BuiltInRegistries.ITEM.getTags().forEach(pair->original.put(pair.getFirst(),pair.getSecond().stream().toList()));
        var tags=new java.util.HashMap<>(original);
        tags.put(TagKey.create(Registries.ITEM,ResourceLocation.fromNamespaceAndPath("c","ingots/steel")),List.of(Items.IRON_INGOT.builtInRegistryHolder(),ModItems.STEEL_INGOT.get().builtInRegistryHolder()));
        tags.put(TagKey.create(Registries.ITEM,ResourceLocation.fromNamespaceAndPath("c","ingots/aluminum")),List.of(Items.COPPER_INGOT.builtInRegistryHolder(),ModItems.ALUMINUM_INGOT.get().builtInRegistryHolder()));
        try {
            BuiltInRegistries.ITEM.bindTags(tags);var yard=yard(h);yard.insert(0,blueprint(BlueprintType.DESTROYER),false);
            add(h,yard,new ItemStack(Items.IRON_INGOT,8));add(h,yard,new ItemStack(Items.COPPER_INGOT,4));add(h,yard,new ItemStack(ModItems.MAIN_GUN_AMMUNITION.get(),8));yard.fluid(null).fill(oil(1000),FluidAction.EXECUTE);
            start(h,yard,"glowworm");yard.cancel(yard.job().id,null);
            h.assertTrue(count(yard,ModItems.STEEL_INGOT.get())==8&&count(yard,ModItems.ALUMINUM_INGOT.get())==4&&count(yard,Items.IRON_INGOT)==0,"common tags accepted, refunds normalized");
        } finally { BuiltInRegistries.ITEM.bindTags(original); }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void blockPaymentCancelDoesNotRefundChangeTwice(GameTestHelper h) {
        var yard=yard(h);yard.insert(0,blueprint(BlueprintType.DESTROYER),false);
        add(h,yard,new ItemStack(ModBlocks.STEEL_BLOCK.get()));add(h,yard,new ItemStack(ModBlocks.ALUMINUM_BLOCK.get()));add(h,yard,new ItemStack(ModItems.MAIN_GUN_AMMUNITION.get(),8));yard.fluid(null).fill(oil(1000),FluidAction.EXECUTE);
        start(h,yard,"glowworm");h.assertTrue(count(yard,ModItems.STEEL_INGOT.get())==1&&count(yard,ModItems.ALUMINUM_INGOT.get())==5,"change before completion");
        yard.cancel(yard.job().id,null);h.assertTrue(count(yard,ModItems.STEEL_INGOT.get())==9&&count(yard,ModItems.ALUMINUM_INGOT.get())==9,"refund only net cost");h.succeed();
    }

    @GameTest(template = "empty")
    public static void changeOverflowDropsWithoutBlockingStart(GameTestHelper h) {
        var yard=yard(h);yard.insert(0,blueprint(BlueprintType.DESTROYER),false);yard.insert(2,new ItemStack(ModBlocks.STEEL_BLOCK.get(),64),false);yard.insert(3,new ItemStack(ModBlocks.ALUMINUM_BLOCK.get(),64),false);
        for(int slot=4;slot<14;slot++)yard.insert(slot,new ItemStack(ModItems.TORPEDO.get(),64),false);
        yard.fluid(null).fill(oil(1000),FluidAction.EXECUTE);var player=player(h);
        for(int i=0;i<36;i++)player.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));
        h.assertTrue(yard.start(id("glowworm"),player)==ShipyardBlockEntity.StartResult.STARTED,"change space never blocks start");
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(yard.getBlockPos()).inflate(4));
        h.assertTrue(drops.stream().filter(e->e.getItem().is(ModItems.STEEL_INGOT.get())).mapToInt(e->e.getItem().getCount()).sum()==1
                &&drops.stream().filter(e->e.getItem().is(ModItems.ALUMINUM_INGOT.get())).mapToInt(e->e.getItem().getCount()).sum()==5,"only change overflows to world");h.succeed();
    }

    @GameTest(template = "empty")
    public static void sideExtractionPrioritizesOutput(GameTestHelper h) {
        var yard=yard(h);supply(h,yard,"glowworm",2);start(h,yard,"glowworm");advance(yard,900);add(h,yard,new ItemStack(ModItems.STEEL_INGOT.get(),3));
        var side=yard.items(Direction.NORTH);
        h.assertTrue(side.extractItem(1,64,false).isEmpty(),"cache cannot bypass pending output");
        h.assertTrue(side.extractItem(0,1,false).is(ModItems.RIGGING_CORE.get()),"output is the first side slot");
        h.assertTrue(side.extractItem(1,64,false).getCount()==3,"cache available after output");
        h.assertTrue(!yard.items(Direction.DOWN).insertItem(0,new ItemStack(ModItems.STEEL_INGOT.get()),false).isEmpty(),"bottom output only");h.succeed();
    }

    @GameTest(template = "empty")
    public static void portableBarrelKeepsOriginalPlacement(GameTestHelper h) {
        h.setBlock(POS,ModBlocks.FUEL_BARREL.get());var player=player(h);var carried=new ItemStack(ModBlocks.FUEL_BARREL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND,carried);var abs=h.absolutePos(POS);
        var hit=new BlockHitResult(Vec3.atCenterOf(abs).add(0,.5,0),Direction.UP,abs,false);
        var result=h.getBlockState(POS).useItemOn(carried,h.getLevel(),player,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(result==ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION,"portable fluid capability does not swallow barrel placement");
        carried.getItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
        h.assertTrue(h.getBlockState(POS.above()).is(ModBlocks.FUEL_BARREL.get())&&player.getMainHandItem().isEmpty(),"last barrel places on existing barrel");h.succeed();
    }

    @GameTest(template = "empty")
    public static void recipeConstraintsAllowLargerBudgets(GameTestHelper h) {
        var large=new ShipyardRecipe(id("glowworm"),32000,1024,128,192,600);
        var encoded=ShipyardRecipe.CODEC.codec().encodeStart(com.mojang.serialization.JsonOps.INSTANCE,large).getOrThrow();
        var decoded=ShipyardRecipe.CODEC.codec().parse(com.mojang.serialization.JsonOps.INSTANCE,encoded).getOrThrow();
        h.assertTrue(decoded.equals(large)&&decoded.ticks()==12000,"codec accepts costs above initial balance guidance");
        var json=encoded.getAsJsonObject();json.addProperty("ammunition",6);
        h.assertTrue(ShipyardRecipe.CODEC.codec().parse(com.mojang.serialization.JsonOps.INSTANCE,json).error().isPresent(),"cost must be a multiple of four");
        h.assertTrue(ManufacturingMaterials.plan(List.of(new ItemStack(ModItems.MISSILE.get(),64)),new ShipyardRecipe(id("glowworm"),0,Integer.MAX_VALUE-3,0,0,1))==null,"large costs do not overflow into free construction");h.succeed();
    }

    @GameTest(template = "empty")
    public static void creativeBreakScattersContentsWithoutShipyard(GameTestHelper h) {
        var yard=yard(h);supply(h,yard,"glowworm",2);start(h,yard,"glowworm");advance(yard,100);
        var player=h.makeMockPlayer(GameType.CREATIVE);var pos=yard.getBlockPos();var state=yard.getBlockState();
        state.getBlock().playerWillDestroy(h.getLevel(),pos,state,player);h.getLevel().removeBlock(pos,false);
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(4));
        h.assertTrue(drops.stream().noneMatch(e->e.getItem().is(ModBlocks.SHIPYARD.get().asItem())||e.getItem().is(ModItems.FUEL_OIL_BUCKET.get())),"no shipyard or oil item");
        h.assertTrue(drops.stream().filter(e->e.getItem().is(ModItems.STEEL_INGOT.get())).mapToInt(e->e.getItem().getCount()).sum()==8
                &&drops.stream().filter(e->e.getItem().is(ModItems.ALUMINUM_INGOT.get())).mapToInt(e->e.getItem().getCount()).sum()==4
                &&drops.stream().filter(e->e.getItem().is(ModItems.MAIN_GUN_AMMUNITION.get())).mapToInt(e->e.getItem().getCount()).sum()==8
                &&drops.stream().filter(e->e.getItem().is(ModItems.SHIP_BLUEPRINT.get())).mapToInt(e->e.getItem().getCount()).sum()==2,"contents and actual payment scattered exactly once");
        h.assertTrue(yard.job()==null&&yard.fuel().isEmpty()&&!yard.hasCachedContents()&&player.getInventory().isEmpty(),"no retained cache, fuel or refund to creative inventory");h.succeed();
    }

    @GameTest(template = "empty")
    public static void creativeBreakScattersPendingCore(GameTestHelper h) {
        var yard=yard(h);supply(h,yard,"glowworm",1);start(h,yard,"glowworm");advance(yard,900);
        var player=h.makeMockPlayer(GameType.CREATIVE);var pos=yard.getBlockPos();var state=yard.getBlockState();
        state.getBlock().playerWillDestroy(h.getLevel(),pos,state,player);h.getLevel().removeBlock(pos,false);
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(4));
        h.assertTrue(drops.size()==1&&RiggingCoreRules.definition(drops.getFirst().getItem()).orElseThrow().id().equals(id("glowworm")),"pending core scattered once without shipyard");h.succeed();
    }
}
