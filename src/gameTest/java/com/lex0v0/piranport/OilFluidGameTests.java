package com.lex0v0.piranport;

import com.lex0v0.piranport.registry.ModFluids;
import com.lex0v0.piranport.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

// 验证自然流动保护水格、水面延展，以及倒桶继续使用原版替换行为。
@GameTestHolder(PiranPort.MODID)
@PrefixGameTestTemplate(false)
public final class OilFluidGameTests {
    private static final BlockPos CENTER = new BlockPos(7, 4, 7);

    private static FlowingFluid oil(boolean refined) {
        return refined ? ModFluids.FUEL_OIL.get() : ModFluids.CRUDE_OIL.get();
    }

    private static void basin(GameTestHelper h) {
        for (int x = 2; x <= 12; x++) {
            for (int z = 2; z <= 12; z++) {
                h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
                for (int y = 2; y <= 9; y++) {
                    boolean wall = x == 2 || x == 12 || z == 2 || z == 12;
                    h.setBlock(new BlockPos(x, y, z), wall ? Blocks.STONE : Blocks.AIR);
                }
            }
        }
    }

    private static void surface(GameTestHelper h, boolean refined) {
        basin(h);
        for (int x = 3; x <= 11; x++) {
            for (int z = 3; z <= 11; z++) {
                h.setBlock(new BlockPos(x, 2, z), Blocks.WATER);
                h.setBlock(new BlockPos(x, 3, z), Blocks.WATER);
            }
        }
        FlowingFluid fluid = oil(refined);
        h.setBlock(CENTER, fluid.defaultFluidState().createLegacyBlock());
        h.runAtTickTime(140, () -> {
            for (int x = 3; x <= 11; x++) {
                for (int z = 3; z <= 11; z++) {
                    for (int y = 2; y <= 3; y++) {
                        h.assertTrue(h.getBlockState(new BlockPos(x, y, z)).equals(Blocks.WATER.defaultBlockState()),
                                "natural oil flow must preserve every water source");
                    }
                }
            }
            h.assertTrue(h.getBlockState(CENTER).getFluidState().isSource(), "surface source stays in place");
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                FluidState edge = h.getBlockState(CENTER.relative(direction, 3)).getFluidState();
                h.assertTrue(fluid.isSame(edge.getType()) && !edge.isSource() && edge.getAmount() == 2,
                        "flowing oil must extend beyond the first surface cell with normal decay");
            }
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 180)
    public static void crudeOilStaysOnWaterAndSpreads(GameTestHelper h) {
        surface(h, false);
    }

    @GameTest(template = "empty", timeoutTicks = 180)
    public static void fuelOilStaysOnWaterAndSpreads(GameTestHelper h) {
        surface(h, true);
    }

    private static List<BlockState> waterStates() {
        List<BlockState> states = new ArrayList<>();
        states.add(Blocks.WATER.defaultBlockState());
        for (int amount = 1; amount <= 7; amount++) {
            states.add(Fluids.FLOWING_WATER.getFlowing(amount, false).createLegacyBlock());
        }
        states.add(Fluids.FLOWING_WATER.getFlowing(8, true).createLegacyBlock());
        states.add(Blocks.OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true));
        return states;
    }

    private static void protectsWaterStates(GameTestHelper h, boolean refined) {
        basin(h);
        FlowingFluid fluid = oil(refined);
        for (BlockState water : waterStates()) {
            for (boolean flowing : new boolean[] {false, true}) {
                h.setBlock(CENTER.west().below(), Blocks.STONE);
                h.setBlock(CENTER.west(), fluid.defaultFluidState().createLegacyBlock());
                h.setBlock(CENTER.below(), water);
                FluidState state = flowing ? fluid.getFlowing(6, false) : fluid.defaultFluidState();
                h.setBlock(CENTER, state.createLegacyBlock());
                state.tick(h.getLevel(), h.absolutePos(CENTER));
                h.assertTrue(h.getBlockState(CENTER.below()).equals(water),
                        "source and flowing oil must not replace source, flowing, falling or waterlogged water");
            }
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void crudeOilPreservesAllWaterStates(GameTestHelper h) {
        protectsWaterStates(h, false);
    }

    @GameTest(template = "empty")
    public static void fuelOilPreservesAllWaterStates(GameTestHelper h) {
        protectsWaterStates(h, true);
    }

    private static void dryFlow(GameTestHelper h, boolean refined) {
        basin(h);
        FlowingFluid fluid = oil(refined);
        h.setBlock(new BlockPos(7, 8, 7), fluid.defaultFluidState().createLegacyBlock());
        h.runAtTickTime(220, () -> {
            h.assertTrue(fluid.isSame(h.getBlockState(new BlockPos(7, 2, 7)).getFluidState().getType()),
                    "oil still falls through air onto dry ground");
            h.assertTrue(fluid.isSame(h.getBlockState(new BlockPos(9, 2, 7)).getFluidState().getType()),
                    "oil still spreads sideways on dry ground");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 260)
    public static void crudeOilStillFallsAndSpreadsOnLand(GameTestHelper h) {
        dryFlow(h, false);
    }

    @GameTest(template = "empty", timeoutTicks = 260)
    public static void fuelOilStillFallsAndSpreadsOnLand(GameTestHelper h) {
        dryFlow(h, true);
    }

    @GameTest(template = "empty")
    public static void shorelineWaterDoesNotStallDryDirections(GameTestHelper h) {
        basin(h);
        for (boolean refined : new boolean[] {false, true}) {
            FlowingFluid fluid = oil(refined);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                h.setBlock(CENTER.relative(direction), Blocks.AIR);
                h.setBlock(CENTER.relative(direction).below(), Blocks.STONE);
            }
            h.setBlock(CENTER.below(), Blocks.STONE);
            h.setBlock(CENTER.east(), Blocks.WATER);
            h.setBlock(CENTER.east().below(), Blocks.WATER);
            FluidState source = fluid.defaultFluidState();
            h.setBlock(CENTER, source.createLegacyBlock());
            source.tick(h.getLevel(), h.absolutePos(CENTER));
            h.assertTrue(h.getBlockState(CENTER.east()).is(Blocks.WATER), "shoreline water is not replaced");
            for (Direction direction : new Direction[] {Direction.WEST, Direction.NORTH, Direction.SOUTH}) {
                h.assertTrue(fluid.isSame(h.getBlockState(CENTER.relative(direction)).getFluidState().getType()),
                        "an invalid water direction must not stall legal dry directions");
            }
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void oilBucketsKeepVanillaWaterReplacement(GameTestHelper h) {
        basin(h);
        for (boolean refined : new boolean[] {false, true}) {
            BucketItem bucket = refined ? ModItems.FUEL_OIL_BUCKET.get() : ModItems.CRUDE_OIL_BUCKET.get();
            h.assertTrue(bucket.getClass() == BucketItem.class, "oil buckets retain the vanilla item class");
            for (BlockState water : waterStates().subList(0, 9)) {
                for (boolean withContainer : new boolean[] {false, true}) {
                    h.setBlock(CENTER, water);
                    boolean placed = withContainer
                            ? bucket.emptyContents(null, h.getLevel(), h.absolutePos(CENTER), null, new ItemStack(bucket))
                            : bucket.emptyContents(null, h.getLevel(), h.absolutePos(CENTER), null);
                    h.assertTrue(placed && oil(refined).isSame(h.getBlockState(CENTER).getFluidState().getType())
                                    && h.getBlockState(CENTER).getFluidState().isSource(),
                            "direct bucket placement must still replace water at the original target");
                }
            }
        }
        h.succeed();
    }
}
