package com.lex0v0.piranport;

import com.lex0v0.piranport.block.FuelBarrelBlock;
import com.lex0v0.piranport.blockentity.FuelBarrelBlockEntity;
import com.lex0v0.piranport.registry.ModBlocks;
import com.lex0v0.piranport.registry.ModDataComponents;
import com.lex0v0.piranport.registry.ModFluids;
import com.lex0v0.piranport.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

// 验证容量配置、降容守恒及随放置持久保存的连接策略。
@GameTestHolder(PiranPort.MODID)
@PrefixGameTestTemplate(false)
public final class FuelBarrelConfigGameTests {
    private static final BlockPos A = new BlockPos(6, 2, 6);

    private static FuelBarrelBlockEntity place(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, ModBlocks.FUEL_BARREL.get().defaultBlockState());
        return (FuelBarrelBlockEntity) h.getBlockEntity(pos);
    }

    private static FuelBarrelBlockEntity placeItem(GameTestHelper h, ItemStack stack) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var target = h.absolutePos(A);
        var hit = new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false);
        var result = ModItems.FUEL_BARREL_ITEM.get().place(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit));
        h.assertTrue(result.consumesAction() && stack.isEmpty(), "real item placement consumes one barrel");
        return (FuelBarrelBlockEntity) h.getBlockEntity(A.above());
    }

    private static FluidStack oil(int amount) {
        return new FluidStack(ModFluids.CRUDE_OIL.get(), amount);
    }

    private static CompoundTag save(GameTestHelper h, FuelBarrelBlockEntity barrel) {
        return barrel.saveWithFullMetadata(h.getLevel().registryAccess());
    }

    private static int local(GameTestHelper h, FuelBarrelBlockEntity barrel) {
        return FluidStack.parseOptional(h.getLevel().registryAccess(), save(h, barrel).getCompound("Fluid")).getAmount();
    }

    @GameTest(template = "empty")
    public static void configuredCapacityAndSimulation(GameTestHelper h) {
        int previous = Config.FUEL_BARREL_CAPACITY.get();
        try {
            Config.FUEL_BARREL_CAPACITY.set(12345);
            var lower = place(h, A);
            var upper = place(h, A.above());
            lower.refreshConnection();
            var handler = lower.getFluidHandler(null);
            var before = save(h, lower);
            h.assertTrue(handler.getTankCapacity(0) == 24690
                    && handler.fill(oil(25000), FluidAction.SIMULATE) == 24690
                    && save(h, lower).equals(before), "custom paired capacity and read-only simulation");
            h.assertTrue(handler.fill(oil(25000), FluidAction.EXECUTE) == 24690
                    && local(h, lower) == 12345 && local(h, upper) == 12345, "custom local and paired limits");
            var single = place(h, A.offset(4, 0, 0));
            h.assertTrue(single.getFluidHandler(null).fill(oil(13000), FluidAction.EXECUTE) == 12345
                    && single.getComparatorOutput() == 15, "non-bucket-multiple capacity is exact");
            h.succeed();
        } finally {
            Config.FUEL_BARREL_CAPACITY.set(previous);
        }
    }

    @GameTest(template = "empty")
    public static void capacityReductionPreservesExistingPair(GameTestHelper h) {
        int previous = Config.FUEL_BARREL_CAPACITY.get();
        try {
            Config.FUEL_BARREL_CAPACITY.set(32000);
            var lower = place(h, A);
            var upper = place(h, A.above());
            lower.refreshConnection();
            var cached = lower.getFluidHandler(null);
            cached.fill(oil(40000), FluidAction.EXECUTE);
            Config.FUEL_BARREL_CAPACITY.set(16000);
            lower.loadWithComponents(save(h, lower), h.getLevel().registryAccess());
            upper.loadWithComponents(save(h, upper), h.getLevel().registryAccess());
            lower.refreshConnection();
            h.assertTrue(cached.getTankCapacity(0) == 40000 && cached.getFluidInTank(0).getAmount() == 40000
                    && save(h, lower).contains("Partner") && save(h, upper).contains("Partner"), "降容与重载保留旧配对和全部超额存量");
            var savedLower = save(h, lower);
            var savedUpper = save(h, upper);
            lower.prepareForPlayerRemoval();
            h.assertTrue(local(h, upper) == 16000 && local(h, lower) == 24000,
                    "超额配对采集仍优先填满剩余桶，其余存量留在被采集桶");
            lower.loadWithComponents(savedLower, h.getLevel().registryAccess());
            upper.loadWithComponents(savedUpper, h.getLevel().registryAccess());
            var before = save(h, lower);
            h.assertTrue(cached.fill(oil(1000), FluidAction.SIMULATE) == 0
                    && cached.fill(oil(1000), FluidAction.EXECUTE) == 0
                    && cached.drain(8000, FluidAction.SIMULATE).getAmount() == 8000
                    && save(h, lower).equals(before), "超额存量不能继续灌入，模拟抽取不改存档");
            cached.drain(8000, FluidAction.EXECUTE);
            lower.refreshConnection();
            h.assertTrue(local(h, lower) == 16000 && local(h, upper) == 16000
                    && cached.getTankCapacity(0) == 32000, "抽出超额后恢复新上限并按格分摊");
            lower.prepareForPlayerRemoval();
            h.assertTrue(local(h, lower) == 16000 && local(h, upper) == 16000, "拆分仍守恒");
            h.succeed();
        } finally {
            Config.FUEL_BARREL_CAPACITY.set(previous);
        }
    }

    @GameTest(template = "empty")
    public static void fillAfterReductionKeepsOverfullLocalShare(GameTestHelper h) {
        int previous = Config.FUEL_BARREL_CAPACITY.get();
        try {
            Config.FUEL_BARREL_CAPACITY.set(32000);
            var lower = place(h, A);
            var upper = place(h, A.above());
            lower.refreshConnection();
            var handler = lower.getFluidHandler(null);
            handler.fill(oil(24000), FluidAction.EXECUTE);
            Config.FUEL_BARREL_CAPACITY.set(16000);
            h.assertTrue(handler.fill(oil(10000), FluidAction.SIMULATE) == 8000 && local(h, lower) == 24000,
                    "pair room is computed without mutating the overfull member");
            h.assertTrue(handler.fill(oil(10000), FluidAction.EXECUTE) == 8000
                    && local(h, lower) == 24000 && local(h, upper) == 8000, "fill skips overfull local tank without truncation");
            lower.refreshConnection();
            h.assertTrue(local(h, lower) == 16000 && local(h, upper) == 16000, "later rebalance preserves all oil");
            h.succeed();
        } finally {
            Config.FUEL_BARREL_CAPACITY.set(previous);
        }
    }

    @GameTest(template = "empty")
    public static void disabledPlacementBlocksBothDirectionsAndPersists(GameTestHelper h) {
        boolean previous = Config.FUEL_BARREL_AXIAL_CONNECTION.get();
        try {
            Config.FUEL_BARREL_AXIAL_CONNECTION.set(true);
            var lower = place(h, A);
            Config.FUEL_BARREL_AXIAL_CONNECTION.set(false);
            var upper = placeItem(h, new ItemStack(ModItems.FUEL_BARREL_ITEM.get()));
            lower.refreshConnection();
            upper.refreshConnection();
            h.assertTrue(!save(h, upper).getBoolean("AxialConnectionAllowed")
                    && !save(h, lower).contains("Partner") && !save(h, upper).contains("Partner"), "disabled new placement cannot be paired by either member");
            upper.loadWithComponents(save(h, upper), h.getLevel().registryAccess());
            Config.FUEL_BARREL_AXIAL_CONNECTION.set(true);
            for (var axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Y}) {
                h.getLevel().setBlock(h.absolutePos(A.above()), upper.getBlockState().setValue(FuelBarrelBlock.AXIS, axis), Block.UPDATE_ALL);
                lower.refreshConnection();
                upper.refreshConnection();
            }
            h.assertTrue(!save(h, upper).contains("Partner") && !save(h, upper).getBoolean("AxialConnectionAllowed"),
                    "reload, axe-style axis changes and re-enabling do not rewrite an existing barrel's policy");
        } finally {
            Config.FUEL_BARREL_AXIAL_CONNECTION.set(previous);
        }
        h.runAfterDelay(3, () -> {
            h.assertTrue(!save(h, (FuelBarrelBlockEntity) h.getBlockEntity(A.above())).contains("Partner"),
                    "scheduled neighbor refresh cannot bypass placement policy");
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void disablingPreservesExistingStates(GameTestHelper h) {
        boolean previous = Config.FUEL_BARREL_AXIAL_CONNECTION.get();
        try {
            Config.FUEL_BARREL_AXIAL_CONNECTION.set(true);
            var lower = place(h, A);
            var upper = place(h, A.above());
            lower.refreshConnection();
            lower.getFluidHandler(null).fill(oil(20000), FluidAction.EXECUTE);
            var first = place(h, A.offset(4, 0, 0));
            var second = place(h, A.offset(4, 1, 0));
            Config.FUEL_BARREL_AXIAL_CONNECTION.set(false);
            lower.loadWithComponents(save(h, lower), h.getLevel().registryAccess());
            upper.loadWithComponents(save(h, upper), h.getLevel().registryAccess());
            lower.refreshConnection();
            h.assertTrue(lower.getStoredFluid().getAmount() == 20000 && upper.getStoredFluid().getAmount() == 20000
                    && lower.getFluidHandler(null).getTankCapacity(0) == 32000, "switching off retains existing shared storage after reload");
            first.refreshConnection();
            h.assertTrue(save(h, first).getBoolean("AxialConnectionAllowed") && save(h, second).contains("Partner"),
                    "existing unpaired barrels retain their original ability to connect");
            var third = place(h, A.above(2));
            third.refreshConnection();
            h.assertTrue(!save(h, third).contains("Partner") && save(h, lower).contains("Partner"), "new independent third barrel cannot disturb old pair");
            h.succeed();
        } finally {
            Config.FUEL_BARREL_AXIAL_CONNECTION.set(previous);
        }
    }

    @GameTest(template = "empty")
    public static void legacyPolicyDefaultsToAllowed(GameTestHelper h) {
        boolean previous = Config.FUEL_BARREL_AXIAL_CONNECTION.get();
        try {
            Config.FUEL_BARREL_AXIAL_CONNECTION.set(false);
            var lower = place(h, A);
            var upper = place(h, A.above());
            for (var barrel : new FuelBarrelBlockEntity[]{lower, upper}) {
                var legacy = save(h, barrel);
                legacy.remove("AxialConnectionAllowed");
                barrel.loadWithComponents(legacy, h.getLevel().registryAccess());
            }
            lower.refreshConnection();
            h.assertTrue(save(h, lower).getBoolean("AxialConnectionAllowed") && save(h, upper).contains("Partner"),
                    "old NBT without a policy keeps pre-configuration connection behavior even while disabled");
            h.succeed();
        } finally {
            Config.FUEL_BARREL_AXIAL_CONNECTION.set(previous);
        }
    }

    @GameTest(template = "empty")
    public static void replacedItemUsesCurrentPolicyAndKeepsOverflow(GameTestHelper h) {
        boolean previous = Config.FUEL_BARREL_AXIAL_CONNECTION.get();
        try {
            Config.FUEL_BARREL_AXIAL_CONNECTION.set(true);
            var lower = place(h, A);
            var carried = new ItemStack(ModItems.FUEL_BARREL_ITEM.get());
            carried.set(ModDataComponents.FLUID_CONTENT.get(), SimpleFluidContent.copyOf(oil(24000)));
            var legacy = save(h, lower);
            var staleNeighbor = legacy.copy();
            staleNeighbor.putLong("Partner", h.absolutePos(A.above()).asLong());
            lower.loadWithComponents(staleNeighbor, h.getLevel().registryAccess());
            legacy.putLong("Partner", h.absolutePos(A).asLong());
            carried.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(legacy));
            Config.FUEL_BARREL_AXIAL_CONNECTION.set(false);
            var upper = placeItem(h, carried);
            lower.refreshConnection();
            var handler = upper.getFluidHandler(null);
            h.assertTrue(!save(h, upper).getBoolean("AxialConnectionAllowed") && !save(h, lower).contains("Partner")
                    && handler.getFluidInTank(0).getAmount() == 24000 && handler.fill(oil(1000), FluidAction.EXECUTE) == 0,
                    "carried NBT cannot bypass current placement policy, overfull item contents survive");
            h.getLevel().removeBlock(h.absolutePos(A.above()), false);
            Config.FUEL_BARREL_AXIAL_CONNECTION.set(true);
            var replacement = new ItemStack(ModItems.FUEL_BARREL_ITEM.get());
            replacement.set(ModDataComponents.FLUID_CONTENT.get(), SimpleFluidContent.copyOf(oil(24000)));
            upper = placeItem(h, replacement);
            h.assertTrue(save(h, upper).getBoolean("AxialConnectionAllowed") && lower.getStoredFluid().getAmount() == 24000
                    && lower.getFluidHandler(null).getTankCapacity(0) == 32000, "re-placement reads enabled config and restores shared contents");
            h.succeed();
        } finally {
            Config.FUEL_BARREL_AXIAL_CONNECTION.set(previous);
        }
    }

    @GameTest(template = "empty")
    public static void capacityIntegerBoundaries(GameTestHelper h) {
        int previous = Config.FUEL_BARREL_CAPACITY.get();
        try {
            h.assertTrue(!Config.FUEL_BARREL_CAPACITY.getSpec().test(0)
                    && !Config.FUEL_BARREL_CAPACITY.getSpec().test(Integer.MAX_VALUE), "config rejects zero and overflowing paired capacities");
            var lower = place(h, A);
            place(h, A.above());
            for (int capacity : new int[]{1, Integer.MAX_VALUE / 2}) {
                Config.FUEL_BARREL_CAPACITY.set(capacity);
                lower.refreshConnection();
                var handler = lower.getFluidHandler(null);
                h.assertTrue(handler.getTankCapacity(0) == 2 * capacity
                        && handler.fill(oil(Integer.MAX_VALUE), FluidAction.EXECUTE) == 2 * capacity
                        && lower.getComparatorOutput() == 15, "paired capacity does not overflow at supported boundaries");
                h.assertTrue(handler.drain(Integer.MAX_VALUE, FluidAction.EXECUTE).getAmount() == 2 * capacity,
                        "full boundary volume can be extracted without overflow");
            }
            h.succeed();
        } finally {
            Config.FUEL_BARREL_CAPACITY.set(previous);
        }
    }
}
