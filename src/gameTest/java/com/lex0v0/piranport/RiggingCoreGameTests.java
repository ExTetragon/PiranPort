package com.lex0v0.piranport;

import com.lex0v0.piranport.block.FuelBarrelBlock;
import com.lex0v0.piranport.blockentity.FuelBarrelBlockEntity;
import com.lex0v0.piranport.item.RiggingCoreItem;
import com.lex0v0.piranport.item.blueprint.BlueprintSubclass;
import com.lex0v0.piranport.item.blueprint.BlueprintType;
import com.lex0v0.piranport.item.rigging.*;
import com.lex0v0.piranport.registry.*;
import com.lex0v0.piranport.domain.ship.*;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

// 核心物品层的持久化、守恒、分类、熟练度与掉落保护回归。
@GameTestHolder(PiranPort.MODID)
@PrefixGameTestTemplate(false)
public final class RiggingCoreGameTests {
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(PiranPort.MODID, path); }
    private static ItemStack core(String path) { return RiggingCoreRules.create(id(path)); }
    private static FluidStack fuel(int amount) { return new FluidStack(ModFluids.FUEL_OIL.get(), amount); }
    private static void requireReject(GameTestHelper h, Runnable action) {
        boolean rejected = false;
        try { action.run(); } catch (RuntimeException expected) { rejected = true; }
        h.assertTrue(rejected, "invalid write must be rejected");
    }
    private static ItemStack roundtrip(GameTestHelper h, ItemStack stack) {
        return ItemStack.parseOptional(h.getLevel().registryAccess(), (CompoundTag)stack.save(h.getLevel().registryAccess()));
    }
    private static ItemStack box(ItemStack child) {
        var box = new ItemStack(Items.SHULKER_BOX);
        box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(child)));
        return box;
    }
    private static ItemStack bundle(ItemStack child) {
        var bundle = new ItemStack(Items.BUNDLE);
        bundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.of(child)));
        return bundle;
    }
    // 临时标签在同一服务端调用内恢复，不修改发行资源或其他用例的环境。
    private static void withTags(Runnable test) {
        Map<TagKey<Item>, List<Holder<Item>>> original = new HashMap<>();
        BuiltInRegistries.ITEM.getTags().forEach(pair -> original.put(pair.getFirst(), pair.getSecond().stream().toList()));
        var tags = new HashMap<>(original);
        tags.put(ModItemTags.WEAPONS, List.of(Items.IRON_SWORD.builtInRegistryHolder(), RiggingTestFixtures.positive.builtInRegistryHolder()));
        tags.put(ModItemTags.AIRCRAFT, List.of(Items.FEATHER.builtInRegistryHolder()));
        tags.put(ModItemTags.DEVICES, List.of(Items.IRON_INGOT.builtInRegistryHolder(), RiggingTestFixtures.negative.builtInRegistryHolder()));
        tags.put(ModItemTags.AMMUNITION, List.of(Items.ARROW.builtInRegistryHolder()));
        try { BuiltInRegistries.ITEM.bindTags(tags); test.run(); }
        finally { BuiltInRegistries.ITEM.bindTags(original); }
    }

    @GameTest(template = "empty")
    public static void factoryAndSharedBlueprintTypes(GameTestHelper h) {
        h.assertTrue(ShipType.values().length == 9 && ShipSize.values().length == 3 && ShipDefinitions.ALL.size() == 10, "catalog sizes");
        for (var ship : ShipDefinitions.ALL) {
            var stack = RiggingCoreRules.create(ship.id());
            h.assertTrue(stack.getItem() == ModItems.RIGGING_CORE.get() && stack.getMaxStackSize() == 1 && !stack.isDamageableItem(), "one registered non-damageable item");
            h.assertTrue(RiggingCoreRules.definition(stack).orElseThrow().equals(ship), "factory identity");
            h.assertTrue(RiggingCoreRules.getProficiency(stack) == 0 && RiggingCoreRules.fuel(stack).getFluid().isEmpty(), "empty factory state");
            for (var region : RiggingRegion.values()) h.assertTrue(RiggingCoreRules.inventory(stack, region).occupiedSlots() == 0, "empty regions");
        }
        for (var type : ShipType.values()) {
            var blueprint = BlueprintType.byId(type.id());
            h.assertTrue(blueprint.shipType() == type && blueprint.translationKey().equals(type.translationKey()), "shared ship definition");
            h.assertTrue(blueprint.subclass() == BlueprintSubclass.forShipSize(type.size()) && blueprint.subclass().id().equals(type.size().id()), "shared size mapping");
        }
        requireReject(h, () -> RiggingCoreRules.create(id("missing")));
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void capacitiesAndRegisteredCapabilities(GameTestHelper h) {
        var paths = List.of("lexington_cv2", "quincy", "u47");
        int[][] expected = {{6,4,9,45,128000,64}, {5,3,8,36,96000,52}, {4,2,6,27,64000,39}};
        for (int index = 0; index < paths.size(); index++) {
            var path = paths.get(index);
            var stack = core(path);
            var size = RiggingCoreRules.definition(stack).orElseThrow().size();
            int sum = 0;
            for (var region : RiggingRegion.values()) {
                h.assertTrue(RiggingCoreRules.inventory(stack, region).getSlots() == region.capacity(size), "regional capacity");
                h.assertTrue(region.capacity(size) == expected[index][region.ordinal()], "independent planned region capacity");
                sum += region.capacity(size);
            }
            h.assertTrue(stack.getCapability(Capabilities.ItemHandler.ITEM).getSlots() == sum, "registered combined inventory");
            h.assertTrue(stack.getCapability(Capabilities.FluidHandler.ITEM).getTankCapacity(0) == size.fuel(), "registered fluid capability");
            h.assertTrue(size.fuel() == expected[index][4] && sum == expected[index][5], "planned fuel and total slots");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void blankAndMismatchedIdentities(GameTestHelper h) {
        var stack = new ItemStack(ModItems.RIGGING_CORE.get());
        h.assertTrue(RiggingCoreRules.status(stack) == RiggingCoreRules.IdentityStatus.MISSING && RiggingCoreRules.inventory(stack).getSlots() == 0, "blank give is not initialized");
        for (var identity : List.of(new RiggingIdentity(id("removed_ship"), ShipType.DESTROYER.id()),
                new RiggingIdentity(id("u47"), id("ship/unknown")), new RiggingIdentity(id("u47"), ShipType.BATTLESHIP.id()))) {
            stack.set(ModDataComponents.RIGGING_IDENTITY, identity);
            stack.set(ModDataComponents.RIGGING_CARGO, ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND, 7))));
            RiggingCoreRules.setProficiency(stack, 99);
            var saved = roundtrip(h, stack);
            h.assertTrue(saved.get(ModDataComponents.RIGGING_IDENTITY).equals(identity), "unknown identity survives save");
            var handler = RiggingCoreRules.inventory(saved, RiggingRegion.CARGO);
            h.assertTrue(handler.insertItem(0, new ItemStack(Items.STONE), false).getCount() == 1, "invalid identity rejects insertion");
            h.assertTrue(handler.extractItem(0, 7, false).getCount() == 7 && RiggingCoreRules.getProficiency(saved) == 99, "invalid identity allows recovery without resetting proficiency");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void fullPersistenceAndNetwork(GameTestHelper h) {
        withTags(() -> {
            var stack = core("lexington_cv2");
            for (var region : RiggingRegion.values()) {
                var handler = RiggingCoreRules.inventory(stack, region);
                var item = switch (region) { case WEAPONS -> Items.IRON_SWORD; case EQUIPMENT -> Items.IRON_INGOT; case AMMUNITION -> Items.ARROW; case CARGO -> Items.DIAMOND; };
                for (int slot = 0; slot < handler.getSlots(); slot++) handler.setStackInSlot(slot, new ItemStack(item, region.singleItem() ? 1 : 64));
            }
            stack.set(DataComponents.CUSTOM_NAME, Component.literal("Saved core"));
            RiggingCoreRules.setProficiency(stack, 177);
            RiggingCoreRules.fuel(stack).fill(fuel(128000), FluidAction.EXECUTE);
            h.assertTrue(ItemStack.matches(stack, roundtrip(h, stack)), "all components survive disk save");
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
            try {
                ItemStack.STREAM_CODEC.encode(buffer, stack);
                h.assertTrue(ItemStack.matches(stack, ItemStack.STREAM_CODEC.decode(buffer)), "full core survives network encoding");
            } finally { buffer.release(); }
        });
        h.assertTrue(ItemStack.matches(core("u47"), roundtrip(h, core("u47"))), "empty core save");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void regionTagsAndSingleItemLimits(GameTestHelper h) {
        withTags(() -> {
            var stack = core("u47");
            var weapons = RiggingCoreRules.inventory(stack, RiggingRegion.WEAPONS);
            h.assertTrue(weapons.insertItem(0, new ItemStack(Items.IRON_SWORD), false).isEmpty(), "weapon tag accepted on small ship");
            h.assertTrue(weapons.insertItem(1, new ItemStack(Items.FEATHER, 12), false).getCount() == 11, "aircraft accepted one per slot");
            h.assertTrue(weapons.insertItem(2, new ItemStack(Items.IRON_INGOT), false).getCount() == 1, "device rejected by weapon slot");
            var devices = RiggingCoreRules.inventory(stack, RiggingRegion.EQUIPMENT);
            h.assertTrue(devices.insertItem(0, new ItemStack(Items.IRON_INGOT, 64), false).getCount() == 63, "one device per slot");
            var ammo = RiggingCoreRules.inventory(stack, RiggingRegion.AMMUNITION);
            h.assertTrue(ammo.insertItem(0, new ItemStack(Items.ARROW, 64), false).isEmpty(), "normal ammunition stack");
            h.assertTrue(!ammo.isItemValid(1, new ItemStack(Items.STONE)), "ammo tag required");
        });
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void directWritesValidateCountsAndBounds(GameTestHelper h) {
        withTags(() -> {
            var handler = RiggingCoreRules.inventory(core("u47"), RiggingRegion.WEAPONS);
            requireReject(h, () -> handler.setStackInSlot(0, new ItemStack(Items.FEATHER, 2)));
            requireReject(h, () -> handler.setStackInSlot(0, new ItemStack(Items.STONE)));
            requireReject(h, () -> handler.getStackInSlot(-1));
            requireReject(h, () -> handler.getStackInSlot(handler.getSlots()));
            handler.setStackInSlot(0, new ItemStack(Items.FEATHER));
            h.assertTrue(handler.extractItem(0, -2, false).isEmpty() && handler.getStackInSlot(0).getCount() == 1, "negative extraction is inert");
            handler.setStackInSlot(0, ItemStack.EMPTY);
            h.assertTrue(handler.getStackInSlot(0).isEmpty(), "clear always permitted");
        });
        var cargo = RiggingCoreRules.inventory(core("u47"), RiggingRegion.CARGO);
        requireReject(h, () -> cargo.setStackInSlot(0, new ItemStack(Items.STONE, 65)));
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void insertionSimulationAndRemainders(GameTestHelper h) {
        var stack = core("u47");
        var handler = RiggingCoreRules.inventory(stack, RiggingRegion.CARGO);
        handler.setStackInSlot(0, new ItemStack(Items.STONE, 60));
        var before = stack.copy();
        var input = new ItemStack(Items.STONE, 10);
        h.assertTrue(handler.insertItem(0, input, true).getCount() == 6 && ItemStack.matches(stack, before) && input.getCount() == 10, "simulate insertion is pure");
        h.assertTrue(handler.insertItem(0, input, false).getCount() == 6 && handler.getStackInSlot(0).getCount() == 64, "partial insertion remainder");
        var named = new ItemStack(Items.STONE);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Distinct"));
        h.assertTrue(handler.insertItem(0, named, false).getCount() == 1, "component mismatch is not merged");
        before = stack.copy();
        h.assertTrue(handler.extractItem(0, 9, true).getCount() == 9 && ItemStack.matches(stack, before), "simulate extraction is pure");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void queryAndCopyIsolation(GameTestHelper h) {
        var stack = core("u47");
        var handler = RiggingCoreRules.inventory(stack, RiggingRegion.CARGO);
        var diamonds = new ItemStack(Items.DIAMOND, 5);
        handler.setStackInSlot(0, diamonds);
        diamonds.setCount(1);
        var read = handler.getStackInSlot(0); read.setCount(2);
        h.assertTrue(handler.getStackInSlot(0).getCount() == 5, "write input and read result are defensive copies");
        var copy = stack.copy();
        RiggingCoreRules.inventory(copy, RiggingRegion.CARGO).extractItem(0, 3, false);
        h.assertTrue(handler.getStackInSlot(0).getCount() == 5, "core copy storage is isolated");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void nestedContainersRejectAllCores(GameTestHelper h) {
        var handler = RiggingCoreRules.inventory(core("u47"), RiggingRegion.CARGO);
        for (var child : List.of(core("quincy"), new ItemStack(ModItems.RIGGING_CORE.get()))) {
            for (var input : List.of(child, box(child), bundle(child), box(bundle(box(child)))))
                h.assertTrue(!handler.isItemValid(0, input) && handler.insertItem(0, input, false).getCount() == 1, "direct and nested cores rejected");
        }
        for (var input : List.of(new ItemStack(Items.SHULKER_BOX), box(new ItemStack(Items.DIAMOND, 64)), bundle(new ItemStack(Items.STONE)))) {
            h.assertTrue(handler.insertItem(0, input, false).isEmpty(), "ordinary containers accepted despite vanilla default filter");
            handler.setStackInSlot(0, ItemStack.EMPTY);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void outerContainersKeepOrdinaryBehavior(GameTestHelper h) {
        var stack = core("u47");
        h.assertTrue(stack.canFitInsideContainerItems(), "core can be stored by outer containers");
        for (var outer : List.of(box(stack), bundle(stack))) {
            h.assertTrue(ItemStack.matches(outer, roundtrip(h, outer)), "outer container preserves core");
            h.assertTrue(outer.canBeHurtBy(h.getLevel().damageSources().cactus()), "outer container inherits no core protection");
            h.assertTrue(outer.getEntityLifespan(h.getLevel()) == 6000, "outer container lifespan is unchanged");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void overflowSlotsAndIdentityRecovery(GameTestHelper h) {
        var stack = core("lexington_cv2");
        var cargo = RiggingCoreRules.inventory(stack, RiggingRegion.CARGO);
        cargo.setStackInSlot(44, new ItemStack(Items.DIAMOND, 5));
        stack.set(ModDataComponents.RIGGING_IDENTITY, new RiggingIdentity(id("u47"), ShipType.SUBMARINE.id()));
        h.assertTrue(cargo.getSlots() == 45 && cargo.getStackInSlot(44).getCount() == 5, "capacity reduction retains excess slots and cached handler resolves current identity");
        h.assertTrue(cargo.insertItem(44, new ItemStack(Items.DIAMOND), false).getCount() == 1, "overflow slot is extraction only");
        h.assertTrue(cargo.extractItem(44, 5, false).getCount() == 5 && cargo.getSlots() == 27, "recovery removes no other content");
        stack.set(ModDataComponents.RIGGING_IDENTITY, new RiggingIdentity(id("missing"), ShipType.SUBMARINE.id()));
        h.assertTrue(cargo.getSlots() == 0, "unknown identity grants no fabricated capacity");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void tagChangesDoNotDeleteStoredEquipment(GameTestHelper h) {
        var stack = core("u47");
        withTags(() -> RiggingCoreRules.inventory(stack, RiggingRegion.WEAPONS).setStackInSlot(0, new ItemStack(Items.FEATHER)));
        var handler = RiggingCoreRules.inventory(stack, RiggingRegion.WEAPONS);
        h.assertTrue(handler.getStackInSlot(0).is(Items.FEATHER), "tag removal retains contents");
        h.assertTrue(handler.insertItem(1, new ItemStack(Items.FEATHER), false).getCount() == 1, "removed tag blocks new insertion");
        h.assertTrue(handler.extractItem(0, 1, false).is(Items.FEATHER), "removed tag still allows extraction");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void fluidFilteringAndSimulation(GameTestHelper h) {
        var stack = core("u47"); var tank = RiggingCoreRules.fuel(stack); var before = stack.copy();
        h.assertTrue(tank.fill(fuel(70000), FluidAction.SIMULATE) == 64000 && ItemStack.matches(stack, before), "fluid simulate leaves item untouched");
        h.assertTrue(tank.fill(new FluidStack(ModFluids.CRUDE_OIL.get(), 1000), FluidAction.EXECUTE) == 0, "reject crude oil");
        h.assertTrue(tank.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE) == 0, "reject water");
        h.assertTrue(tank.fill(fuel(70000), FluidAction.EXECUTE) == 64000 && tank.fill(fuel(1), FluidAction.EXECUTE) == 0, "capacity respected");
        before = stack.copy();
        h.assertTrue(tank.drain(33, FluidAction.SIMULATE).getAmount() == 33 && ItemStack.matches(stack, before), "drain simulate pure");
        requireReject(h, () -> tank.getTankCapacity(1));
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void fluidComponentsAndEmptyEncoding(GameTestHelper h) {
        var stack = core("u47"); var tank = RiggingCoreRules.fuel(stack);
        var named = fuel(1001); named.set(DataComponents.CUSTOM_NAME, Component.literal("Tagged fuel"));
        h.assertTrue(tank.fill(named, FluidAction.EXECUTE) == 1001 && named.getAmount() == 1001, "input fluid untouched");
        h.assertTrue(tank.fill(fuel(500), FluidAction.EXECUTE) == 0, "fluid components prevent mixing");
        h.assertTrue(tank.drain(fuel(500), FluidAction.EXECUTE).isEmpty(), "component-aware drain");
        var copy = tank.getFluid(); copy.setAmount(5);
        h.assertTrue(tank.getFluid().getAmount() == 1001, "fluid query returns copy");
        var drained = tank.drain(400, FluidAction.EXECUTE);
        h.assertTrue(drained.getAmount() == 400 && drained.has(DataComponents.CUSTOM_NAME) && tank.getFluid().getAmount() == 601, "partial drain preserves components");
        tank.drain(Integer.MAX_VALUE, FluidAction.EXECUTE);
        h.assertTrue(RiggingCoreRules.fuel(roundtrip(h, stack)).getFluid().isEmpty(), "empty fluid content encodes safely");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void fuelOverfillAndInvalidIdentityRecovery(GameTestHelper h) {
        var stack = core("u47"); var tank = RiggingCoreRules.fuel(stack);
        stack.set(ModDataComponents.FLUID_CONTENT, SimpleFluidContent.copyOf(fuel(150000)));
        h.assertTrue(tank.fill(fuel(100), FluidAction.EXECUTE) == 0 && tank.getFluid().getAmount() == 150000, "overfill is preserved with nonnegative rejection");
        stack.remove(ModDataComponents.RIGGING_IDENTITY);
        h.assertTrue(tank.getTankCapacity(0) == 0 && tank.fill(fuel(1), FluidAction.EXECUTE) == 0, "invalid identity cannot fill");
        h.assertTrue(tank.drain(150000, FluidAction.EXECUTE).getAmount() == 150000 && RiggingCoreRules.isCore(stack), "recovery drain leaves core intact");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void realFuelBarrelTransfer(GameTestHelper h) {
        var pos = new BlockPos(2, 2, 2);
        h.setBlock(pos, ModBlocks.FUEL_BARREL.get().defaultBlockState().setValue(FuelBarrelBlock.AXIS, Direction.Axis.Y));
        var barrel = (FuelBarrelBlockEntity)h.getBlockEntity(pos);
        barrel.getFluidHandler(null).fill(fuel(13001), FluidAction.EXECUTE);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var stack = core("u47");
        RiggingCoreRules.inventory(stack, RiggingRegion.CARGO).setStackInSlot(0, new ItemStack(Items.DIAMOND, 4));
        RiggingCoreRules.setProficiency(stack, 83);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var abs = h.absolutePos(pos);
        var hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
        h.getBlockState(pos).useItemOn(player.getMainHandItem(), h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        h.assertTrue(barrel.getStoredFluid().isEmpty() && RiggingCoreRules.fuel(player.getMainHandItem()).getFluid().getAmount() == 13001, "barrel fills held core");
        h.getBlockState(pos).useItemOn(player.getMainHandItem(), h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        h.assertTrue(barrel.getStoredFluid().getAmount() == 13001 && RiggingCoreRules.fuel(player.getMainHandItem()).getFluid().isEmpty(), "core refills barrel");
        h.assertTrue(RiggingCoreRules.inventory(player.getMainHandItem(), RiggingRegion.CARGO).getStackInSlot(0).getCount() == 4 && RiggingCoreRules.getProficiency(player.getMainHandItem()) == 83, "real transfer preserves unrelated data");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void additiveStatsExcludeCargoAndProficiency(GameTestHelper h) {
        withTags(() -> {
            var stack = core("u47");
            RiggingCoreRules.inventory(stack, RiggingRegion.WEAPONS).setStackInSlot(0, new ItemStack(RiggingTestFixtures.positive));
            RiggingCoreRules.inventory(stack, RiggingRegion.EQUIPMENT).setStackInSlot(0, new ItemStack(RiggingTestFixtures.negative));
            RiggingCoreRules.inventory(stack, RiggingRegion.EQUIPMENT).setStackInSlot(1, new ItemStack(Items.IRON_INGOT));
            RiggingCoreRules.inventory(stack, RiggingRegion.CARGO).setStackInSlot(0, new ItemStack(RiggingTestFixtures.positive));
            RiggingCoreRules.setProficiency(stack, 200);
            var before = stack.copy();
            var stats = RiggingStatsCalculator.calculate(stack).orElseThrow();
            h.assertTrue(stats.durability() == 16 && stats.armor() == 2 && Math.abs(stats.speed() - 3.75) < 1e-8, "additive corrections, no final speed floor, cargo/proficiency/plain tagged item excluded");
            h.assertTrue(ItemStack.matches(stack, before), "stats query has no effects");
        });
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void everyShipBaseline(GameTestHelper h) {
        double[] speeds = {7.04, 7.04, 6.6, 6.6, 6.94, 6.8, 4.4, 7.6, 8.2, 4};
        int[] health = {32,32,38,38,22,28,10,10,16,10};
        int[] armor = {4,4,8,6,4,5,2,1,2,1};
        for (int i = 0; i < ShipDefinitions.ALL.size(); i++) {
            var stats = RiggingStatsCalculator.calculate(RiggingCoreRules.create(ShipDefinitions.ALL.get(i).id())).orElseThrow();
            h.assertTrue(Math.abs(stats.speed() - speeds[i]) < 1e-8 && stats.durability() == health[i] && stats.armor() == armor[i], "ship baseline " + i);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void tooltipAndOccupancyArePure(GameTestHelper h) {
        var stack = core("u47");
        RiggingCoreRules.inventory(stack, RiggingRegion.CARGO).setStackInSlot(0, box(new ItemStack(Items.STONE, 64)));
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Renamed"));
        var before = stack.copy();
        var simple = new ArrayList<Component>();
        stack.getItem().appendHoverText(stack, Item.TooltipContext.EMPTY, simple, TooltipFlag.NORMAL);
        h.assertTrue(simple.size() == 1 && ((TranslatableContents)simple.getFirst().getContents()).getKey().equals("item.piranport.rigging_core"), "simple tooltip contains only core type");
        var details = RiggingTooltips.details(stack);
        h.assertTrue(details.size() == 11, "shift detail field count");
        var classification = (TranslatableContents)details.get(1).getContents();
        h.assertTrue(classification.getKey().equals("tooltip.piranport.rigging.size") && classification.getArgs().length == 2,
                "size and type share one detail line");
        h.assertTrue(((TranslatableContents)details.get(2).getContents()).getKey().endsWith("proficiency"), "proficiency follows ship classification");
        h.assertTrue(RiggingCoreRules.inventory(stack, RiggingRegion.CARGO).occupiedSlots() == 1, "nested items count as one slot");
        h.assertTrue(ItemStack.matches(stack, before) && stack.getItem().getName(stack).getStyle().getColor().getValue() == RiggingCoreItem.NAME_COLOR, "query purity and default name color");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void proficiencyBoundariesAndPreservation(GameTestHelper h) {
        var stack = core("u47");
        stack.remove(ModDataComponents.RIGGING_PROFICIENCY);
        var before = stack.copy();
        h.assertTrue(RiggingCoreRules.getProficiency(stack) == 0 && ItemStack.matches(stack, before), "missing component reads zero without writing");
        requireReject(h, () -> RiggingCoreRules.setProficiency(stack, -1));
        requireReject(h, () -> RiggingCoreRules.setProficiency(stack, 201));
        requireReject(h, () -> RiggingCoreRules.addProficiency(stack, -1));
        RiggingCoreRules.setProficiency(stack, 199);
        h.assertTrue(RiggingCoreRules.addProficiency(stack, Integer.MAX_VALUE) == 1 && RiggingCoreRules.getProficiency(stack) == 200, "saturates without integer overflow");
        h.assertTrue(RiggingCoreRules.addProficiency(stack, 1) == 0, "max proficiency stops increasing");
        var copy = roundtrip(h, stack); RiggingCoreRules.setProficiency(copy, 17);
        h.assertTrue(RiggingCoreRules.getProficiency(stack) == 200 && RiggingCoreRules.getProficiency(copy) == 17, "proficiency copies are independent");
        requireReject(h, () -> RiggingCoreRules.setProficiency(new ItemStack(Items.STONE), 1));
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void environmentalDamageProtection(GameTestHelper h) {
        for (var stack : List.of(core("u47"), new ItemStack(ModItems.RIGGING_CORE.get()))) {
            var entity = new ItemEntity(h.getLevel(), h.absolutePos(new BlockPos(2, 3, 2)).getX(), h.absolutePos(new BlockPos(2,3,2)).getY(), h.absolutePos(new BlockPos(2,3,2)).getZ(), stack);
            var sources = h.getLevel().damageSources();
            for (var damage : List.of(sources.cactus(), sources.lava(), sources.inFire(), sources.onFire(), sources.explosion(null, null), sources.fall(), sources.lightningBolt()))
                h.assertTrue(!entity.hurt(damage, 10000) && !entity.isRemoved(), "environmental damage immunity independent of identity");
            h.assertTrue(entity.fireImmune(), "fire resistant item property");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void lifetimeAndLoadedEntityPersistence(GameTestHelper h) {
        var position = Vec3.atCenterOf(h.absolutePos(new BlockPos(2, 4, 2)));
        var stack = core("u47");
        RiggingCoreRules.inventory(stack, RiggingRegion.CARGO).setStackInSlot(0, new ItemStack(Items.DIAMOND, 3));
        var entity = new ItemEntity(h.getLevel(), position.x, position.y, position.z, stack);
        entity.setNoGravity(true);
        entity.setDeltaMovement(Vec3.ZERO);
        h.getLevel().addFreshEntity(entity);
        for (int i = 0; i < 6010; i++) entity.tick();
        h.assertTrue(!entity.isRemoved() && entity.getAge() == -32768, "no natural expiration after normal lifetime");
        var nbt = new CompoundTag(); entity.save(nbt);
        var loaded = new ItemEntity(h.getLevel(), position.x + 2, position.y, position.z, new ItemStack(Items.STONE));
        loaded.load(nbt);
        h.assertTrue(loaded.getAge() == -32768 && ItemStack.matches(loaded.getItem(), stack), "save/load preserves marker and contents");
        nbt.putShort("Age", (short)5999); nbt.putInt("Lifespan", 6000);
        loaded.load(nbt); loaded.setUUID(java.util.UUID.randomUUID()); h.getLevel().addFreshEntity(loaded);
        h.assertTrue(loaded.getAge() == -32768, "world join repairs lifespan before the first item tick");
        loaded.tick();
        h.assertTrue(loaded.getAge() == -32768 && !loaded.isRemoved(), "old finite-lifespan drop repaired on joining world");
        entity.discard(); loaded.discard();
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void hopperCollectionAndVoid(GameTestHelper h) {
        var position = Vec3.atCenterOf(h.absolutePos(new BlockPos(2,4,2)));
        var stack = core("u47");
        RiggingCoreRules.setProficiency(stack, 55);
        var entity = new ItemEntity(h.getLevel(), position.x, position.y, position.z, stack.copy());
        var inventory = new net.minecraft.world.SimpleContainer(5);
        h.assertTrue(HopperBlockEntity.addItem(inventory, entity) && entity.isRemoved() && ItemStack.matches(inventory.getItem(0), stack), "normal hopper collection preserves core");
        var falling = new ItemEntity(h.getLevel(), position.x, h.getLevel().getMinBuildHeight() - 100, position.z, stack.copy());
        falling.tick();
        h.assertTrue(falling.isRemoved(), "void removal remains allowed");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void creativeExamplesAndIcon(GameTestHelper h) {
        var tab = ModCreativeTabs.MAIN.get();
        tab.buildContents(new net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters(h.getLevel().enabledFeatures(), true, h.getLevel().registryAccess()));
        var cores = tab.getDisplayItems().stream().filter(RiggingCoreRules::isCore).toList();
        h.assertTrue(cores.size() == 10 && cores.stream().allMatch(s -> RiggingCoreRules.status(s) == RiggingCoreRules.IdentityStatus.VALID && RiggingCoreRules.getProficiency(s) == 0), "ten valid creative examples");
        h.assertTrue(RiggingCoreRules.definition(tab.getIconItem()).orElseThrow().id().equals(id("lexington_cv2")), "Lexington is the mod tab icon");
        h.succeed();
    }
}
