package com.lex0v0.piranport;

import com.lex0v0.piranport.block.FuelBarrelBlock;
import com.lex0v0.piranport.blockentity.FuelBarrelBlockEntity;
import com.lex0v0.piranport.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(PiranPort.MODID)
@PrefixGameTestTemplate(false)
public final class BarrelInteractionGameTests {
    private static final BlockPos A = new BlockPos(6, 2, 6);
    private static FuelBarrelBlockEntity place(GameTestHelper h, BlockPos pos, Direction.Axis axis) {
        h.setBlock(pos, ModBlocks.FUEL_BARREL.get().defaultBlockState().setValue(FuelBarrelBlock.AXIS, axis));
        return (FuelBarrelBlockEntity) h.getBlockEntity(pos);
    }
    private static int local(GameTestHelper h, FuelBarrelBlockEntity barrel) {
        return FluidStack.parseOptional(h.getLevel().registryAccess(),
                barrel.saveWithFullMetadata(h.getLevel().registryAccess()).getCompound("Fluid")).getAmount();
    }
    private static void use(GameTestHelper h, BlockPos pos, Player player, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var target = h.absolutePos(pos);
        h.getBlockState(pos).useItemOn(stack, h.getLevel(), player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false));
    }

    @GameTest(template = "empty")
    public static void bucketTransferAndRejection(GameTestHelper h) {
        var barrel = place(h, A, Direction.Axis.Y);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        for (var oil : java.util.List.of(ModItems.CRUDE_OIL_BUCKET.get(), ModItems.FUEL_OIL_BUCKET.get())) {
            for (int n = 0; n < 32; n++) {
                use(h, A, player, new ItemStack(oil));
                h.assertTrue(player.getMainHandItem().is(Items.BUCKET)
                        && barrel.getStoredFluid().getAmount() == (n + 1) * 1000, "bucket fills 1000 mB");
            }
            use(h, A, player, new ItemStack(oil));
            h.assertTrue(player.getMainHandItem().is(oil) && barrel.getStoredFluid().getAmount() == 32000, "full barrel rejects without consuming bucket");
            for (int n = 31; n >= 0; n--) {
                use(h, A, player, new ItemStack(Items.BUCKET));
                h.assertTrue(player.getMainHandItem().is(oil)
                        && barrel.getStoredFluid().getAmount() == n * 1000, "empty bucket extracts 1000 mB");
            }
            use(h, A, player, new ItemStack(Items.BUCKET));
            h.assertTrue(player.getMainHandItem().is(Items.BUCKET) && barrel.getStoredFluid().isEmpty(), "empty barrel creates no oil");
        }
        use(h, A, player, new ItemStack(ModItems.CRUDE_OIL_BUCKET.get()));
        use(h, A, player, new ItemStack(ModItems.FUEL_OIL_BUCKET.get()));
        h.assertTrue(player.getMainHandItem().is(ModItems.FUEL_OIL_BUCKET.get()) && barrel.getStoredFluid().getAmount() == 1000,
                "different oil rejected without consuming bucket");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void horizontalMasterAndCapacity(GameTestHelper h) {
        for (var axis : java.util.List.of(Direction.Axis.X, Direction.Axis.Z)) {
            var pos = axis == Direction.Axis.X ? A : A.offset(8, 0, 8);
            var first = place(h, pos, axis);
            var nbt = first.saveWithFullMetadata(h.getLevel().registryAccess());
            nbt.putLong("PlaceOrder", 1);
            first.loadWithComponents(nbt, h.getLevel().registryAccess());
            var second = place(h, axis == Direction.Axis.X ? pos.west() : pos.north(), axis);
            nbt = second.saveWithFullMetadata(h.getLevel().registryAccess());
            nbt.putLong("PlaceOrder", 2);
            second.loadWithComponents(nbt, h.getLevel().registryAccess());
            first.refreshConnection();
            var handler = first.getFluidHandler(null);
            h.assertTrue(first.getMaster() == first && second.getMaster() == first, "earlier placement wins even at larger coordinate");
            h.assertTrue(handler.fill(new FluidStack(ModFluids.CRUDE_OIL.get(), 65000), FluidAction.EXECUTE) == 64000
                    && handler.fill(new FluidStack(ModFluids.CRUDE_OIL.get(), 1000), FluidAction.EXECUTE) == 0, "pair has exact 64000 capacity");
            h.assertTrue(local(h, first) == 32000 && local(h, second) == 32000, "both local shares capped");
            handler.drain(1000, FluidAction.EXECUTE);
            h.assertTrue(local(h, first) == 32000 && local(h, second) == 31000, "drain non-master first");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void axeCyclesSplitAndReconnect(GameTestHelper h) {
        var lower = place(h, A, Direction.Axis.Y);
        var upper = place(h, A.above(), Direction.Axis.Y);
        lower.refreshConnection();
        lower.getFluidHandler(null).fill(new FluidStack(ModFluids.CRUDE_OIL.get(), 40000), FluidAction.EXECUTE);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        for (var axis : java.util.List.of(Direction.Axis.X, Direction.Axis.Z, Direction.Axis.Y)) {
            use(h, A, player, new ItemStack(Items.IRON_AXE));
            h.assertTrue(h.getBlockState(A).getValue(FuelBarrelBlock.AXIS) == axis, "axe cycle Y-X-Z-Y");
            h.assertTrue(local(h, lower) + local(h, upper) == 40000, "axis change conserves local shares");
            h.assertTrue(lower.getFluidHandler(null).getTankCapacity(0) == (axis == Direction.Axis.Y ? 64000 : 32000), "only matching axis reconnects");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void mixedAxesAndRemovedHandler(GameTestHelper h) {
        var first = place(h, A, Direction.Axis.Y);
        var second = place(h, A.above(), Direction.Axis.X);
        first.refreshConnection();
        second.refreshConnection();
        h.assertTrue(first.getFluidHandler(null).getTankCapacity(0) == 32000
                && second.getFluidHandler(null).getTankCapacity(0) == 32000, "mixed axes remain independent");
        var cached = first.getFluidHandler(null);
        h.getLevel().setBlock(h.absolutePos(A), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        h.assertTrue(cached.fill(new FluidStack(ModFluids.CRUDE_OIL.get(), 1000), FluidAction.EXECUTE) == 0
                && cached.drain(1000, FluidAction.EXECUTE).isEmpty(), "removed entity rejects stale handler operations");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void commandReplacementKeepsNeighborShare(GameTestHelper h) {
        var lower = place(h, A, Direction.Axis.Y);
        var upper = place(h, A.above(), Direction.Axis.Y);
        lower.refreshConnection();
        lower.getFluidHandler(null).fill(new FluidStack(ModFluids.CRUDE_OIL.get(), 40000), FluidAction.EXECUTE);
        h.getLevel().setBlock(h.absolutePos(A), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        h.assertTrue(upper.getStoredFluid().getAmount() == 8000 && upper.getFluidHandler(null).getTankCapacity(0) == 32000,
                "replacement discards only removed local share");
        h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(A)).inflate(2)).isEmpty(), "replacement creates no drops");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void creativeBreakAndPiston(GameTestHelper h) {
        var barrel = place(h, A, Direction.Axis.Y);
        barrel.setStoredFluid(new FluidStack(ModFluids.CRUDE_OIL.get(), 1000));
        h.assertTrue(barrel.getBlockState().getPistonPushReaction() == PushReaction.BLOCK, "piston cannot move barrel");
        var player = h.makeMockServerPlayerInLevel();
        player.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
        h.assertTrue(player.gameMode.destroyBlock(h.absolutePos(A)), "creative player destroys barrel");
        h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(A)).inflate(2)).isEmpty(), "creative destruction creates no drop");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void hopperCannotFeedBarrel(GameTestHelper h) {
        place(h, A, Direction.Axis.Y);
        h.setBlock(A.above(), Blocks.HOPPER);
        var hopper = (HopperBlockEntity) h.getBlockEntity(A.above());
        hopper.setItem(0, new ItemStack(ModItems.CRUDE_OIL_BUCKET.get()));
        h.assertTrue(h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(A), Direction.UP) == null,
                "barrel has no item capability");
        h.assertTrue(h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(A), Direction.UP) != null,
                "barrel exposes fluid capability");
        var furnacePos = A.offset(8, 0, 0);
        h.setBlock(furnacePos, Blocks.FURNACE);
        h.setBlock(furnacePos.above(), Blocks.HOPPER);
        var feeder = (HopperBlockEntity) h.getBlockEntity(furnacePos.above());
        feeder.setItem(0, new ItemStack(ModItems.CRUDE_OIL_BUCKET.get()));
        h.runAfterDelay(25, () -> {
            var furnace = (net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity) h.getBlockEntity(furnacePos);
            h.assertTrue(feeder.getItem(0).isEmpty() && furnace.getItem(0).is(ModItems.CRUDE_OIL_BUCKET.get()), "hopper feeds oil item into furnace input");
            h.assertTrue(hopper.getItem(0).is(ModItems.CRUDE_OIL_BUCKET.get()), "real hopper retains oil bucket");
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void enchantmentEdgeCases(GameTestHelper h) {
        var registry = h.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        var tool = new ItemStack(Items.DIAMOND_PICKAXE);
        tool.enchant(registry.getHolderOrThrow(Enchantments.FORTUNE), 10);
        for (var block : java.util.List.of(ModBlocks.BAUXITE_ORE.get(), ModBlocks.SALTPETER_BLOCK.get())) {
            h.setBlock(A, block);
            for (int i = 0; i < 100; i++) {
                var drops = Block.getDrops(block.defaultBlockState(), h.getLevel(), h.absolutePos(A), null, null, tool);
                int count = drops.stream().mapToInt(ItemStack::getCount).sum();
                h.assertTrue(count >= (block == ModBlocks.BAUXITE_ORE.get() ? 1 : 2)
                        && count <= (block == ModBlocks.BAUXITE_ORE.get() ? 33 : 4), "fortune X stays within formula limits");
            }
        }
        tool.enchant(registry.getHolderOrThrow(Enchantments.SILK_TOUCH), 1);
        for (var block : java.util.List.of(ModBlocks.BAUXITE_ORE.get(), ModBlocks.SALTPETER_BLOCK.get())) {
            h.setBlock(A, block);
            var drops = Block.getDrops(block.defaultBlockState(), h.getLevel(), h.absolutePos(A), null, null, tool);
            h.assertTrue(drops.size() == 1 && drops.getFirst().is(block.asItem()) && drops.getFirst().getCount() == 1,
                    "silk touch overrides fortune without duplicate drop");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void placementFacesAndAxis(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var faces = new Direction[]{Direction.UP, Direction.DOWN, Direction.EAST, Direction.NORTH};
        for (int i = 0; i < faces.length; i++) {
            var pos = A.offset(i * 3, 0, 0);
            var clicked = pos.relative(faces[i].getOpposite());
            h.setBlock(clicked, Blocks.STONE);
            player.setYRot(i == 2 ? -90 : 0);
            var stack = new ItemStack(ModItems.FUEL_BARREL_ITEM.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var target = h.absolutePos(clicked);
            var hit = new BlockHitResult(Vec3.atCenterOf(target), faces[i], target, false);
            var result = ModItems.FUEL_BARREL_ITEM.get().place(new net.minecraft.world.item.context.BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit));
            var expected = i < 2 ? Direction.Axis.Y : i == 2 ? Direction.Axis.X : Direction.Axis.Z;
            h.assertTrue(result.consumesAction() && stack.isEmpty() && h.getBlockState(pos).getValue(FuelBarrelBlock.AXIS) == expected,
                    "real placement face and facing select axis: " + faces[i]);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void comparatorEmptyHalfFull(GameTestHelper h) {
        var barrel = place(h, A, Direction.Axis.Y);
        for (int amount : new int[]{0, 16000, 32000}) {
            barrel.setStoredFluid(amount == 0 ? FluidStack.EMPTY : new FluidStack(ModFluids.CRUDE_OIL.get(), amount));
            int expected = amount == 0 ? 0 : amount == 16000 ? 7 : 15;
            h.assertTrue(ModBlocks.FUEL_BARREL.get().getAnalogOutputSignal(barrel.getBlockState(), h.getLevel(), h.absolutePos(A)) == expected,
                    "block comparator output: " + expected);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void emptyHandLeavesBarrelUnchanged(GameTestHelper h) {
        var barrel = place(h, A, Direction.Axis.Y);
        barrel.setStoredFluid(new FluidStack(ModFluids.CRUDE_OIL.get(), 1000));
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var menu = player.containerMenu;
        use(h, A, player, ItemStack.EMPTY);
        var target = h.absolutePos(A);
        h.getBlockState(A).useWithoutItem(h.getLevel(), player, new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false));
        h.assertTrue(player.containerMenu == menu && barrel.getStoredFluid().getAmount() == 1000 && player.getMainHandItem().isEmpty(),
                "empty hand opens no menu and changes no fluid or item");
        h.succeed();
    }
}
