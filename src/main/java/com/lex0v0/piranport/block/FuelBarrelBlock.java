package com.lex0v0.piranport.block;

import com.lex0v0.piranport.blockentity.FuelBarrelBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;

public class FuelBarrelBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

    public FuelBarrelBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.Y));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(FuelBarrelBlock::new); }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(AXIS); }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction.Axis axis = context.getClickedFace().getAxis() == Direction.Axis.Y
                ? Direction.Axis.Y : context.getHorizontalDirection().getAxis();
        return defaultBlockState().setValue(AXIS, axis);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new FuelBarrelBlockEntity(pos, state); }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof FuelBarrelBlockEntity barrel) barrel.refreshConnection();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FuelBarrelBlockEntity barrel)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (stack.is(ItemTags.AXES)) {
            if (!level.isClientSide) {
                barrel.detachForAxisChange();
                Direction.Axis next = switch (state.getValue(AXIS)) {
                    case Y -> Direction.Axis.X;
                    case X -> Direction.Axis.Z;
                    case Z -> Direction.Axis.Y;
                };
                level.setBlock(pos, state.setValue(AXIS, next), Block.UPDATE_ALL);
                barrel.refreshConnection();
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (FluidUtil.getFluidHandler(stack).isPresent()) {
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            return FluidUtil.interactWithFluidHandler(player, hand, barrel.getFluidHandler(hit.getDirection()))
                    ? ItemInteractionResult.CONSUME : ItemInteractionResult.FAIL;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) { return true; }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof FuelBarrelBlockEntity barrel ? barrel.getComparatorOutput() : 0;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FuelBarrelBlockEntity barrel) {
            barrel.prepareForPlayerRemoval();
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FuelBarrelBlockEntity barrel) {
            if (!state.is(newState.getBlock())) barrel.disconnect();
            else if (state.getValue(AXIS) != newState.getValue(AXIS)) barrel.detachForAxisChange();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) level.scheduleTick(pos, this, 1);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean movedByPiston) {
        if (!level.isClientSide) level.scheduleTick(pos, this, 1);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof FuelBarrelBlockEntity barrel) barrel.refreshConnection();
    }
}
