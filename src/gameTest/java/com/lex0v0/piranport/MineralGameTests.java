package com.lex0v0.piranport;

import com.lex0v0.piranport.block.FuelBarrelBlock;
import com.lex0v0.piranport.blockentity.FuelBarrelBlockEntity;
import com.lex0v0.piranport.item.FuelBarrelItem;
import com.lex0v0.piranport.registry.ModBlocks;
import com.lex0v0.piranport.registry.ModDataComponents;
import com.lex0v0.piranport.registry.ModFeatures;
import com.lex0v0.piranport.registry.ModFluids;
import com.lex0v0.piranport.registry.ModItems;
import com.lex0v0.piranport.worldgen.OilLakeConfiguration;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("piranport")
@PrefixGameTestTemplate(false)
/** Restored from the historical regression bytecode; assertions retain their original scope. */
public final class MineralGameTests {
    private static final BlockPos A = new BlockPos(3, 2, 3);
    private static final BlockPos B = A.above();
    private static final FluidAction EXECUTE = FluidAction.EXECUTE;
    private static final FluidAction SIMULATE = FluidAction.SIMULATE;

    private static FuelBarrelBlockEntity barrel(GameTestHelper h, BlockPos pos, Axis axis) {
        h.setBlock(pos, (ModBlocks.FUEL_BARREL.get()).defaultBlockState().setValue(FuelBarrelBlock.AXIS, axis));
        return (FuelBarrelBlockEntity)h.getBlockEntity(pos);
    }

    private static FluidStack oil(int amount) {
        return new FluidStack(ModFluids.CRUDE_OIL.get(), amount);
    }

    private static int amount(FuelBarrelBlockEntity barrel) {
        return barrel.getStoredFluid().getAmount();
    }

    private static int localAmount(GameTestHelper h, FuelBarrelBlockEntity barrel) {
        CompoundTag nbt = barrel.saveWithFullMetadata(h.getLevel().registryAccess());
        return FluidStack.parseOptional(h.getLevel().registryAccess(), nbt.getCompound("Fluid")).getAmount();
    }

    private static FuelBarrelBlockEntity pair(GameTestHelper h) {
        barrel(h, B, Axis.Y);
        FuelBarrelBlockEntity lower = barrel(h, A, Axis.Y);
        lower.refreshConnection();
        return lower;
    }

    @GameTest(template = "empty")
    public static void emptyAndSharedBarrelsSave(GameTestHelper h) {
        FuelBarrelBlockEntity lower = pair(h);
        FuelBarrelBlockEntity upper = (FuelBarrelBlockEntity)h.getBlockEntity(B);
        lower.saveWithFullMetadata(h.getLevel().registryAccess());
        upper.saveWithFullMetadata(h.getLevel().registryAccess());
        lower.getFluidHandler(null).fill(oil(40000), EXECUTE);
        h.assertTrue(localAmount(h, lower) == 32000 && localAmount(h, upper) == 8000, "each member must save its own share");
        CompoundTag savedA = lower.saveWithFullMetadata(h.getLevel().registryAccess());
        CompoundTag savedB = upper.saveWithFullMetadata(h.getLevel().registryAccess());
        lower.loadWithComponents(savedA, h.getLevel().registryAccess());
        upper.loadWithComponents(savedB, h.getLevel().registryAccess());
        lower.refreshConnection();
        h.assertTrue(amount(lower) == 40000 && amount(upper) == 40000, "shared volume must survive NBT roundtrip");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void singleItemPlacementRestoresFluid(GameTestHelper h) {
        h.setBlock(A.below(), Blocks.STONE);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stack = new ItemStack(ModItems.FUEL_BARREL_ITEM.get());
        stack.set(ModDataComponents.FLUID_CONTENT.get(), SimpleFluidContent.copyOf(oil(24000)));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos ground = h.absolutePos(A.below());
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(ground), Direction.UP, ground, false);
        InteractionResult result = (ModItems.FUEL_BARREL_ITEM.get()).place(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit));
        FuelBarrelBlockEntity placed = (FuelBarrelBlockEntity)h.getBlockEntity(A);
        h.assertTrue(result.consumesAction() && stack.isEmpty() && amount(placed) == 24000, "last item must restore before consumption");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void stackedPlacementMergesWithoutOverwrite(GameTestHelper h) {
        FuelBarrelBlockEntity lower = barrel(h, A, Axis.Y);
        lower.setStoredFluid(oil(12000));
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stack = new ItemStack(ModItems.FUEL_BARREL_ITEM.get(), 2);
        stack.set(ModDataComponents.FLUID_CONTENT.get(), SimpleFluidContent.copyOf(oil(8000)));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos target = h.absolutePos(A);
        (ModItems.FUEL_BARREL_ITEM.get())
            .place(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false)));
        h.assertTrue(stack.getCount() == 1 && amount(lower) == 20000, "new barrel must add to existing oil");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void thirdBarrelPreservesExistingPair(GameTestHelper h) {
        FuelBarrelBlockEntity lower = pair(h);
        IFluidHandler tank = lower.getFluidHandler(null);
        h.assertTrue(tank.fill(oil(64000), EXECUTE) == 64000, "pair capacity");
        FuelBarrelBlockEntity third = barrel(h, B.above(), Axis.Y);
        third.refreshConnection();
        lower.refreshConnection();
        h.assertTrue(tank.getTankCapacity(0) == 64000 && amount(lower) == 64000, "third must not dissolve pair");
        h.assertTrue(third.getFluidHandler(null).getTankCapacity(0) == 32000 && amount(third) == 0, "third stays independent");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void incompatibleOilNeverConnects(GameTestHelper h) {
        FuelBarrelBlockEntity lower = barrel(h, A, Axis.X);
        FuelBarrelBlockEntity upper = barrel(h, B, Axis.Y);
        lower.setStoredFluid(oil(12000));
        upper.setStoredFluid(new FluidStack(ModFluids.FUEL_OIL.get(), 14000));
        h.getLevel().setBlock(h.absolutePos(A), lower.getBlockState().setValue(FuelBarrelBlock.AXIS, Axis.Y), 3);
        lower.refreshConnection();
        upper.refreshConnection();
        h.assertTrue(amount(lower) == 12000 && amount(upper) == 14000, "mixed oils must remain independently accessible");
        h.assertTrue(lower.getFluidHandler(null).getTankCapacity(0) == 32000, "mixed oils must not gain shared capacity");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void cachedHandlerFollowsPairAndSplit(GameTestHelper h) {
        FuelBarrelBlockEntity upper = barrel(h, B, Axis.Y);
        IFluidHandler cached = upper.getFluidHandler(null);
        FuelBarrelBlockEntity lower = barrel(h, A, Axis.Y);
        lower.refreshConnection();
        cached.fill(oil(40000), EXECUTE);
        upper.detachForAxisChange();
        h.getLevel().setBlock(h.absolutePos(B), upper.getBlockState().setValue(FuelBarrelBlock.AXIS, Axis.X), 3);
        upper.refreshConnection();
        h.assertTrue(cached == upper.getFluidHandler(null) && cached.getTankCapacity(0) == 32000, "handler identity must stay stable");
        cached.drain(1000, EXECUTE);
        h.assertTrue(amount(lower) == 32000 && amount(upper) == 7000, "cached handler must operate its own member after split");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void fluidChangesMarkChunkAndComparator(GameTestHelper h) {
        FuelBarrelBlockEntity barrel = barrel(h, A, Axis.Y);
        LevelChunk chunk = h.getLevel().getChunkAt(h.absolutePos(A));
        chunk.setUnsaved(false);
        barrel.getFluidHandler(null).fill(oil(16000), EXECUTE);
        h.assertTrue(chunk.isUnsaved(), "fill must dirty chunk");
        h.assertTrue(barrel.getBlockState().hasAnalogOutputSignal() && barrel.getComparatorOutput() == 7, "half-full comparator");
        chunk.setUnsaved(false);
        barrel.getFluidHandler(null).drain(16000, EXECUTE);
        h.assertTrue(chunk.isUnsaved() && barrel.getComparatorOutput() == 0, "drain must dirty chunk and reset comparator");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void simulationDoesNotChangeVolume(GameTestHelper h) {
        FuelBarrelBlockEntity lower = pair(h);
        IFluidHandler tank = lower.getFluidHandler(null);
        h.assertTrue(tank.fill(oil(70000), SIMULATE) == 64000 && amount(lower) == 0, "fill simulation");
        tank.fill(oil(40000), EXECUTE);
        h.assertTrue(tank.drain(50000, SIMULATE).getAmount() == 40000 && amount(lower) == 40000, "drain simulation");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void playerBreakPreservesRemainderInDrop(GameTestHelper h) {
        FuelBarrelBlockEntity lower = pair(h);
        lower.getFluidHandler(null).fill(oil(40000), EXECUTE);
        BlockPos pos = h.absolutePos(A);
        BlockState state = lower.getBlockState();
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        state.getBlock().playerWillDestroy(h.getLevel(), pos, state, player);
        h.getLevel().removeBlock(pos, false);
        state.getBlock().playerDestroy(h.getLevel(), player, pos, state, lower, tool);
        FuelBarrelBlockEntity remaining = (FuelBarrelBlockEntity)h.getBlockEntity(B);
        int dropped = h.getLevel()
            .getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2.0))
            .stream()
            .<ItemStack>map(ItemEntity::getItem)
            .filter(s -> s.is(ModItems.FUEL_BARREL_ITEM.get()))
            .map(s -> s.get(ModDataComponents.FLUID_CONTENT.get()))
            .filter(s -> s != null)
            .mapToInt(SimpleFluidContent::getAmount)
            .sum();
        h.assertTrue(amount(remaining) == 32000 && dropped == 8000, "remaining plus drop must equal original volume");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void nonPlayerRemovalDoesNotDuplicateOil(GameTestHelper h) {
        FuelBarrelBlockEntity lower = pair(h);
        lower.getFluidHandler(null).fill(oil(40000), EXECUTE);
        h.getLevel().destroyBlock(h.absolutePos(A), true);
        FuelBarrelBlockEntity remaining = (FuelBarrelBlockEntity)h.getBlockEntity(B);
        int dropped = h.getLevel()
            .getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(A)).inflate(2.0))
            .stream()
            .<ItemStack>map(ItemEntity::getItem)
            .map(s -> s.get(ModDataComponents.FLUID_CONTENT.get()))
            .filter(s -> s != null)
            .mapToInt(SimpleFluidContent::getAmount)
            .sum();
        h.assertTrue(
            amount(remaining) + dropped == 40000 && remaining.getFluidHandler(null).getTankCapacity(0) == 32000,
            "loot-before-removal must not double-transfer fluid"
        );
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void legacySharedStorageMigratesLosslessly(GameTestHelper h) {
        FuelBarrelBlockEntity lower = barrel(h, A, Axis.Y);
        CompoundTag old = lower.saveWithFullMetadata(h.getLevel().registryAccess());
        old.put("Fluid", oil(64000).save(h.getLevel().registryAccess()));
        old.putInt("Capacity", 64000);
        old.remove("StorageVersion");
        lower.loadWithComponents(old, h.getLevel().registryAccess());
        h.assertTrue(amount(lower) == 64000 && lower.getFluidHandler(null).fill(oil(1000), EXECUTE) == 0, "legacy overflow must not be truncated or refillable");
        FuelBarrelBlockEntity upper = barrel(h, B, Axis.Y);
        lower.refreshConnection();
        h.assertTrue(localAmount(h, lower) == 32000 && localAmount(h, upper) == 32000, "legacy master must redistribute on reconnection");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void storageLootAndItemTagsExist(GameTestHelper h) {
        for (Block block : List.of(ModBlocks.BAUXITE_BLOCK.get(), ModBlocks.ALUMINUM_BLOCK.get(), ModBlocks.STEEL_BLOCK.get())) {
            h.setBlock(A, block);
            List<ItemStack> drops = Block.getDrops(block.defaultBlockState(), h.getLevel(), h.absolutePos(A), null, null, new ItemStack(Items.DIAMOND_PICKAXE));
            h.assertTrue(drops.size() == 1 && drops.getFirst().is(block.asItem()), "storage block must drop itself");
            h.assertTrue(
                block.asItem().builtInRegistryHolder().is(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "storage_blocks"))),
                "storage item parent tag"
            );
        }

        h.assertTrue(
            (ModItems.BAUXITE_ORE_ITEM.get())
                .builtInRegistryHolder()
                .is(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "ores/bauxite"))),
            "ore item tag"
        );
        h.succeed();
    }

    private static void terrain(GameTestHelper h) {
        for (BlockPos pos : BlockPos.betweenClosed(1, 1, 1, 20, 5, 20)) {
            h.setBlock(pos, Blocks.STONE);
        }
    }

    private static boolean oilLake(GameTestHelper h) {
        OilLakeConfiguration config = new OilLakeConfiguration(
            BlockStateProvider.simple(ModBlocks.CRUDE_OIL_BLOCK.get()), BlockStateProvider.simple(Blocks.SANDSTONE), 3, 3, 3
        );
        return new ConfiguredFeature<>(ModFeatures.OIL_LAKE.get(), config)
            .place(h.getLevel(), h.getLevel().getChunkSource().getGenerator(), RandomSource.create(42L), h.absolutePos(new BlockPos(10, 6, 10)));
    }

    @GameTest(template = "empty")
    public static void oilLakeHasDirectFloorAndSealedSides(GameTestHelper h) {
        terrain(h);
        h.assertTrue(oilLake(h), "valid flat terrain must generate a lake");
        h.assertBlockPresent(ModBlocks.CRUDE_OIL_BLOCK.get(), 10, 4, 10);
        h.assertBlockPresent(Blocks.SANDSTONE, 10, 1, 10);

        for (BlockPos pos : BlockPos.betweenClosed(6, 2, 6, 14, 5, 14)) {
            if (h.getBlockState(pos).is(ModBlocks.CRUDE_OIL_BLOCK.get())) {
                for (Direction side : Direction.values()) {
                    if (side != Direction.UP) {
                        BlockState edge = h.getBlockState(pos.relative(side));
                        h.assertTrue(edge.is(ModBlocks.CRUDE_OIL_BLOCK.get()) || edge.is(Blocks.SANDSTONE), "every bottom/side must be sealed");
                    }
                }
            }
        }

        h.succeed();
    }

    @GameTest(template = "empty")
    public static void oilLakeAcceptsGentleNaturalSlopes(GameTestHelper h) {
        terrain(h);

        for (BlockPos pos : BlockPos.betweenClosed(11, 5, 7, 14, 5, 13)) {
            h.setBlock(pos, Blocks.AIR);
        }

        h.assertTrue(oilLake(h), "one-block natural slope should still permit a sealed basin");
        h.assertBlockPresent(ModBlocks.CRUDE_OIL_BLOCK.get(), 10, 4, 10);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void differentFluidComponentsDoNotMerge(GameTestHelper h) {
        FuelBarrelBlockEntity lower = barrel(h, A, Axis.X);
        FuelBarrelBlockEntity upper = barrel(h, B, Axis.Y);
        FluidStack named = oil(12000);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("reserved"));
        lower.setStoredFluid(named);
        upper.setStoredFluid(oil(8000));
        h.getLevel().setBlock(h.absolutePos(A), lower.getBlockState().setValue(FuelBarrelBlock.AXIS, Axis.Y), 3);
        lower.refreshConnection();
        h.assertTrue(amount(lower) == 12000 && amount(upper) == 8000, "component-distinct oil must remain separate");
        SimpleFluidContent component = lower.collectComponents().get(ModDataComponents.FLUID_CONTENT.get());
        h.assertTrue(component != null && component.copy().get(DataComponents.CUSTOM_NAME) != null, "item component must preserve fluid metadata");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void oilLakeProtectsBlockEntitiesBeforeAnyWrite(GameTestHelper h) {
        terrain(h);
        h.setBlock(new BlockPos(10, 4, 10), Blocks.CHEST);
        ChestBlockEntity chest = (ChestBlockEntity)h.getBlockEntity(new BlockPos(10, 4, 10));
        chest.setItem(0, new ItemStack(Items.DIAMOND, 8));
        h.assertTrue(!oilLake(h), "lake must reject protected contents");
        h.assertTrue(chest.getItem(0).getCount() == 8, "chest contents must remain intact");
        h.assertBlockPresent(Blocks.STONE, 10, 5, 10);
        h.assertBlockPresent(Blocks.STONE, 10, 2, 10);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void oilLakeRejectsLeakingEdgesAndBedrock(GameTestHelper h) {
        terrain(h);
        h.setBlock(new BlockPos(13, 4, 10), Blocks.AIR);
        h.assertTrue(!oilLake(h), "open side must reject lake");
        h.assertBlockPresent(Blocks.STONE, 10, 5, 10);
        h.setBlock(new BlockPos(13, 4, 10), Blocks.STONE);
        h.setBlock(new BlockPos(10, 1, 10), Blocks.BEDROCK);
        h.assertTrue(!oilLake(h), "bedrock must not be replaced");
        h.assertBlockPresent(Blocks.STONE, 10, 5, 10);
        h.succeed();
    }
}
