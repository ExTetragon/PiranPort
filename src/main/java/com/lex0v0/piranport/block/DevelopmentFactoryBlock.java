package com.lex0v0.piranport.block;

import com.lex0v0.piranport.blockentity.DevelopmentFactoryBlockEntity;
import com.lex0v0.piranport.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;

// 开发工厂负责打开菜单、世界加取油及拆除前取消任务。
public final class DevelopmentFactoryBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public DevelopmentFactoryBlock(Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(DevelopmentFactoryBlock::new); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new DevelopmentFactoryBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.DEVELOPMENT_FACTORY.get(), DevelopmentFactoryBlockEntity::tick);
    }

    private void open(Level level, BlockPos pos, Player player) {
        if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof DevelopmentFactoryBlockEntity factory)
            server.openMenu(factory, buffer -> { buffer.writeBlockPos(pos); buffer.writeNbt(factory.remembered().save()); });
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) open(level, pos, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!FluidUtil.getFluidHandler(stack).isPresent()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof DevelopmentFactoryBlockEntity factory) {
            boolean inject = factory.fuel().getAmount() < DevelopmentFactoryBlockEntity.FUEL_CAPACITY;
            var result = factory.transferContainer(stack, inject, player);
            if (result.isSuccess()) player.setItemInHand(hand, result.getResult());
            else open(level, pos, player);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof DevelopmentFactoryBlockEntity factory) {
            // 无法采集时战利品表不执行，缓存须按创造拆除散落，避免整机内容丢失。
            if (player.isCreative() || !state.canHarvestBlock(level, pos, player)) factory.destroyCreative();
            else factory.cancelCurrent(player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof DevelopmentFactoryBlockEntity factory) {
            var entity = params.getOptionalParameter(LootContextParams.THIS_ENTITY);
            factory.cancelCurrent(entity instanceof Player player ? player : null);
        }
        return super.getDrops(state, params);
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!state.is(next.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof DevelopmentFactoryBlockEntity factory) factory.cancelCurrent(null);
        super.onRemove(state, level, pos, next, moved);
    }
}
