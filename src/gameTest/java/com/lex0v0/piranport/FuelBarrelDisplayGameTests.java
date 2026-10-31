package com.lex0v0.piranport;

import com.lex0v0.piranport.block.FuelBarrelBlock;
import com.lex0v0.piranport.blockentity.FuelBarrelBlockEntity;
import com.lex0v0.piranport.registry.ModBlocks;
import com.lex0v0.piranport.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

// 验证显示汇总、空间分段及显示更新不改变原有储存事务。
@GameTestHolder(PiranPort.MODID)
@PrefixGameTestTemplate(false)
public final class FuelBarrelDisplayGameTests {
    private static final BlockPos A = new BlockPos(5, 2, 5);

    private static FuelBarrelBlockEntity place(GameTestHelper h, BlockPos p, Direction.Axis axis) {
        h.setBlock(p, ModBlocks.FUEL_BARREL.get().defaultBlockState().setValue(FuelBarrelBlock.AXIS, axis));
        return (FuelBarrelBlockEntity) h.getBlockEntity(p);
    }

    @GameTest(template = "empty")
    public static void sharedDisplayUsesSpatialPartsOnAllAxes(GameTestHelper h) {
        for (Direction.Axis axis : Direction.Axis.values()) {
            BlockPos p = A.offset(axis.ordinal() * 4, 0, 0);
            BlockPos next = p.relative(Direction.get(Direction.AxisDirection.POSITIVE, axis));
            var high = place(h, next, axis);
            high.refreshConnection();
            var low = place(h, p, axis);
            low.refreshConnection();
            high.getFluidHandler(null).fill(new FluidStack(ModFluids.FUEL_OIL.get(), 16000), FluidAction.EXECUTE);
            h.assertTrue(low.getBlockState().getValue(FuelBarrelBlock.PART) == FuelBarrelBlock.Part.LOWER
                    && high.getBlockState().getValue(FuelBarrelBlock.PART) == FuelBarrelBlock.Part.UPPER, "spatial part is independent of master");
            h.assertTrue(low.getDisplayCapacity() == 32000 && low.getDisplayFill() == .5 && high.getDisplayFill() == .5, "shared display uses total capacity");
            h.assertTrue(low.getUpdateTag(h.getLevel().registryAccess()).equals(high.getUpdateTag(h.getLevel().registryAccess())), "both members sync the same summary");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void displaySimulationAndPacketDoNotChangeStorage(GameTestHelper h) {
        var barrel = place(h, A, Direction.Axis.Y);
        barrel.setStoredFluid(new FluidStack(ModFluids.CRUDE_OIL.get(), 8000));
        var before = barrel.saveWithFullMetadata(h.getLevel().registryAccess());
        var tag = barrel.getUpdateTag(h.getLevel().registryAccess());
        barrel.getFluidHandler(null).fill(new FluidStack(ModFluids.CRUDE_OIL.get(), 1000), FluidAction.SIMULATE);
        barrel.getFluidHandler(null).drain(2000, FluidAction.SIMULATE);
        barrel.getDisplayFluid().setAmount(1);
        h.assertTrue(before.equals(barrel.saveWithFullMetadata(h.getLevel().registryAccess()))
                && tag.equals(barrel.getUpdateTag(h.getLevel().registryAccess())), "simulation and display copies have no side effects");
        h.assertTrue(barrel.getUpdatePacket() != null && tag.getInt("DisplayCapacity") == 16000, "network snapshot exists");
        barrel.getFluidHandler(null).drain(1000, FluidAction.EXECUTE);
        h.assertTrue(barrel.getDisplayFluid().getAmount() == 7000, "executed transfer changes display");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void splitAndRemovalRestoreSingleWithoutResurrection(GameTestHelper h) {
        var low = place(h, A, Direction.Axis.Y);
        var high = place(h, A.above(), Direction.Axis.Y);
        low.refreshConnection();
        low.getFluidHandler(null).fill(new FluidStack(ModFluids.FUEL_OIL.get(), 24000), FluidAction.EXECUTE);
        h.setBlock(A, Blocks.AIR);
        h.assertTrue(h.getBlockState(A).isAir(), "display update cannot restore a removed barrel");
        h.assertTrue(high.getBlockState().getValue(FuelBarrelBlock.PART) == FuelBarrelBlock.Part.SINGLE
                && high.getDisplayCapacity() == 16000 && high.getDisplayFluid().getAmount() == 8000, "remaining local contents and single appearance survive");
        var other = place(h, A.above().east(), Direction.Axis.Y);
        high.detachForAxisChange();
        h.setBlock(A.above(), high.getBlockState().setValue(FuelBarrelBlock.AXIS, Direction.Axis.X));
        high.refreshConnection();
        h.assertTrue(high.getBlockState().getValue(FuelBarrelBlock.PART) == FuelBarrelBlock.Part.SINGLE
                && other.getDisplayFluid().isEmpty(), "mixed axes stay separate");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void excessFluidCapsDisplayWithoutTruncation(GameTestHelper h) {
        int previous = Config.FUEL_BARREL_CAPACITY.get();
        try {
            var barrel = place(h, A, Direction.Axis.Y);
            barrel.setStoredFluid(new FluidStack(ModFluids.CRUDE_OIL.get(), 16000));
            Config.FUEL_BARREL_CAPACITY.set(4000);
            barrel.refreshConnection();
            h.assertTrue(barrel.getDisplayFill() == 1 && barrel.getDisplayCapacity() == 4000
                    && barrel.getStoredFluid().getAmount() == 16000, "visual ratio caps at one while excess remains stored");
            barrel.getFluidHandler(null).drain(14000, FluidAction.EXECUTE);
            h.assertTrue(barrel.getDisplayFill() == .5, "draining excess restores half display");
            h.succeed();
        } finally { Config.FUEL_BARREL_CAPACITY.set(previous); }
    }
}
