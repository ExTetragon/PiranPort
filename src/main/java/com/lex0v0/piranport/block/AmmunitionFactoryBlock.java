package com.lex0v0.piranport.block;

import com.lex0v0.piranport.blockentity.AmmunitionFactoryBlockEntity;
import com.lex0v0.piranport.registry.ModBlockEntities;
import com.lex0v0.piranport.registry.ModDataComponents;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

// 世界交互只打开菜单；采集与其他移除分别保存或散落缓存。
public final class AmmunitionFactoryBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public AmmunitionFactoryBlock(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(FACING,net.minecraft.core.Direction.NORTH));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return simpleCodec(AmmunitionFactoryBlock::new);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block,BlockState> b){b.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new AmmunitionFactoryBlockEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:createTickerHelper(t,ModBlockEntities.AMMUNITION_FACTORY.get(),AmmunitionFactoryBlockEntity::tick);}
    private void open(Level l,BlockPos p,Player player){if(player instanceof ServerPlayer server&&l.getBlockEntity(p) instanceof AmmunitionFactoryBlockEntity f)server.openMenu(f,b->{b.writeBlockPos(p);b.writeNbt(f.remembered().save());});}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){if(!l.isClientSide)open(l,p,player);return InteractionResult.sidedSuccess(l.isClientSide);}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;}
    @Override public BlockState playerWillDestroy(Level l,BlockPos p,BlockState s,Player player){
        if(!l.isClientSide&&l.getBlockEntity(p) instanceof AmmunitionFactoryBlockEntity f)f.prepareHarvest(!player.isCreative()&&s.canHarvestBlock(l,p,player));
        return super.playerWillDestroy(l,p,s,player);
    }
    @Override protected List<ItemStack> getDrops(BlockState s,LootParams.Builder params){
        var drops=super.getDrops(s,params);
        if(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof AmmunitionFactoryBlockEntity f&&!f.harvestDrop())
            for(ItemStack stack:drops)stack.remove(ModDataComponents.AMMUNITION_FACTORY_STORAGE.get());
        return drops;
    }
    @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moved){
        if(!s.is(next.getBlock())&&!l.isClientSide&&l.getBlockEntity(p) instanceof AmmunitionFactoryBlockEntity f)f.discardForRemoval();
        super.onRemove(s,l,p,next,moved);
    }
}
