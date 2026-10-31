package com.lex0v0.piranport.block;

import com.lex0v0.piranport.blockentity.ShipyardBlockEntity;
import com.lex0v0.piranport.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Map;
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
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidUtil;

// 船坞负责打开菜单、世界加取油及拆除前取消任务。
public final class ShipyardBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final Map<Direction, VoxelShape> SHAPES = createShapes();

    private static Map<Direction, VoxelShape> createShapes() {
        // 与发行模型的元素一一对应；四向缓存由资源回归检查同步。
        VoxelShape north = Shapes.or(
                box(0, 0, 0, 16, 1.5, 16),
                box(2, 1.5, 0, 14, 2, 14),
                box(0, 1.5, 0, 2, 5.5, 16),
                box(14, 1.5, 0, 16, 5.5, 16),
                box(2, 1.5, 14, 14, 5, 16),
                box(0, 5.5, 0, 2, 6, 16),
                box(0.5, 6, 1, 1, 6.25, 13),
                box(14, 5.5, 0, 16, 6, 16),
                box(14.5, 6, 1, 15, 6.25, 13),
                box(2, 1.5, 0, 14, 2, 0.5),
                box(4, 2, 1.5, 4.5, 2.25, 13.5),
                box(11, 2, 1.5, 11.5, 2.25, 13.5),
                box(5, 2.25, 4.5, 11, 3.1, 5.5),
                box(5, 2.25, 9.5, 11, 3.1, 10.5),
                box(5.6, 3.0, 2.325, 10.4, 4.12, 12.675),
                box(3.8, 3.84, 4.625, 12.2, 5.52, 12.675),
                box(4.7, 3.84, 2.325, 11.3, 5.52, 4.625),
                box(5.9, 4.12, 1.175, 10.1, 5.52, 2.325),
                box(4.7, 3.84, 12.675, 11.3, 5.52, 13.825),
                box(3.2, 5.52, 4.625, 12.8, 6.92, 12.675),
                box(4.4, 5.52, 2.325, 11.6, 6.92, 4.625),
                box(5.6, 5.52, 1.175, 10.4, 6.92, 2.325),
                box(4.1, 5.52, 12.675, 11.9, 6.92, 13.825),
                box(3.8, 6.92, 4.625, 12.2, 7.2, 12.675),
                box(5.0, 6.92, 2.325, 11.0, 7.2, 4.625),
                box(6.2, 6.92, 1.463, 9.8, 7.2, 2.325),
                box(3.2, 6.92, 4.625, 3.8, 7.48, 12.675),
                box(12.2, 6.92, 4.625, 12.8, 7.48, 12.675),
                box(5.9, 7.2, 8.075, 10.1, 8.32, 10.95),
                box(6.5, 8.32, 8.65, 9.5, 9.44, 10.375),
                box(6.2, 9.44, 8.363, 9.8, 9.72, 10.662),
                box(4.4, 7.2, 11.525, 5.0, 8.32, 12.1),
                box(11.0, 7.2, 11.525, 11.6, 8.32, 12.1),
                box(4.4, 8.04, 11.525, 11.6, 8.32, 12.1),
                box(0.75, 6, 11.25, 3, 6.5, 14),
                box(1, 6.5, 11.5, 3, 13.75, 13.5),
                box(1, 6.5, 11.25, 1.5, 13.75, 11.5),
                box(13, 6, 11.25, 15.25, 6.5, 14),
                box(13, 6.5, 11.5, 15, 13.75, 13.5),
                box(13, 6.5, 11.25, 13.5, 13.75, 11.5),
                box(1, 13.75, 11.25, 15, 15.25, 13.75),
                box(2.5, 13.25, 12, 13.5, 13.75, 13),
                box(8.5, 12.5, 11.75, 10.5, 13.25, 13.25),
                box(9.375, 9.75, 12.375, 9.625, 12.5, 12.625),
                box(9.125, 9.25, 12.25, 9.875, 9.75, 12.75),
                box(0.25, 6, 5, 2.25, 6.5, 7),
                box(0.5, 6.5, 5.25, 2, 14.25, 6.75),
                box(0.25, 14.25, 5, 2.25, 15.5, 7),
                box(2.25, 14.25, 5.25, 7.5, 15.25, 6.75),
                box(6, 13.75, 5.5, 7.25, 14.25, 6.5),
                box(6.5, 9, 5.875, 6.75, 13.75, 6.125),
                box(6.25, 8.5, 5.75, 6.75, 9, 6.25),
                box(6.75, 8.5, 5.75, 7.25, 8.75, 6.25),
                box(14, 6, 0.5, 16, 8.5, 3),
                box(14.25, 6.75, 0.35, 15.75, 8, 0.5),
                box(14.75, 3, 13.5, 15.75, 5.5, 15)).optimize();
        VoxelShape east = clockwise(north), south = clockwise(east), west = clockwise(south);
        return Map.of(Direction.NORTH, north, Direction.EAST, east, Direction.SOUTH, south, Direction.WEST, west);
    }

    private static VoxelShape clockwise(VoxelShape shape) {
        VoxelShape[] rotated = {Shapes.empty()};
        shape.forAllBoxes((x0, y0, z0, x1, y1, z1) -> rotated[0] = Shapes.or(rotated[0],
                Shapes.box(1-z1, y0, x0, 1-z0, y1, x1)));
        return rotated[0].optimize();
    }

    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override protected boolean useShapeForLightOcclusion(BlockState state) { return true; }

    public ShipyardBlock(Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(ShipyardBlock::new); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ShipyardBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.SHIPYARD.get(), ShipyardBlockEntity::tick);
    }

    private void open(Level level, BlockPos pos, Player player) {
        if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof ShipyardBlockEntity shipyard)
            server.openMenu(shipyard, buffer -> { buffer.writeBlockPos(pos); buffer.writeNbt(shipyard.remembered().save()); });
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) open(level, pos, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!FluidUtil.getFluidHandler(stack).isPresent()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ShipyardBlockEntity shipyard) {
            boolean inject = shipyard.fuel().getAmount() < ShipyardBlockEntity.FUEL_CAPACITY;
            var result = shipyard.transferContainer(stack, inject, player);
            if (result.isSuccess()) player.setItemInHand(hand, result.getResult());
            else open(level, pos, player);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ShipyardBlockEntity shipyard) {
            // 无法采集时战利品表不执行，缓存须按创造拆除散落，避免整机内容丢失。
            if (player.isCreative() || !state.canHarvestBlock(level, pos, player)) shipyard.destroyCreative();
            else shipyard.cancelCurrent(player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof ShipyardBlockEntity shipyard) {
            var entity = params.getOptionalParameter(LootContextParams.THIS_ENTITY);
            shipyard.cancelCurrent(entity instanceof Player player ? player : null);
        }
        return super.getDrops(state, params);
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!state.is(next.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof ShipyardBlockEntity shipyard) shipyard.cancelCurrent(null);
        super.onRemove(state, level, pos, next, moved);
    }
}
