package com.martinus.piranport.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.IForgeShearable;

public class WildVegeBlock extends BushBlock implements IForgeShearable {
    public WildVegeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return pState.is(Blocks.GRASS_BLOCK);
    }

    @Override
    public OffsetType getOffsetType() {
        return BlockBehaviour.OffsetType.XZ;
    }

}
