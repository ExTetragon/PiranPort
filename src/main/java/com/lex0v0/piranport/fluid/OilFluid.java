package com.lex0v0.piranport.fluid;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

// 两种油共用水面流动规则，桶放置继续走原版路径。
public abstract class OilFluid extends BaseFlowingFluid {
    protected OilFluid(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean canSpreadTo(BlockGetter level, BlockPos fromPos, BlockState fromState,
            Direction direction, BlockPos toPos, BlockState toState, FluidState toFluid, Fluid fluid) {
        return !toFluid.is(FluidTags.WATER)
                && super.canSpreadTo(level, fromPos, fromState, direction, toPos, toState, toFluid, fluid);
    }

    @Override
    protected void spread(Level level, BlockPos pos, FluidState state) {
        if (state.isEmpty() || !level.getFluidState(pos.below()).is(FluidTags.WATER)) {
            super.spread(level, pos, state);
            return;
        }
        // 水面是支撑面，流动态也继续横向衰减，而不是把下方水当作落水孔。
        int remaining = state.getValue(FALLING) ? 7 : state.getAmount() - getDropOff(level);
        if (remaining <= 0) return;
        BlockState fromState = level.getBlockState(pos);
        for (var entry : getSpread(level, pos, fromState).entrySet()) {
            BlockPos target = pos.relative(entry.getKey());
            BlockState targetState = level.getBlockState(target);
            FluidState flowing = entry.getValue();
            if (canSpreadTo(level, pos, fromState, entry.getKey(), target, targetState,
                    targetState.getFluidState(), flowing.getType())) {
                spreadTo(level, target, targetState, entry.getKey(), flowing);
            }
        }
    }

    @Override
    protected Map<Direction, FluidState> getSpread(Level level, BlockPos pos, BlockState state) {
        Map<Direction, FluidState> spread = new EnumMap<>(Direction.class);
        spread.putAll(super.getSpread(level, pos, state));
        boolean waterRemoved = spread.keySet().removeIf(
                direction -> level.getFluidState(pos.relative(direction)).is(FluidTags.WATER));
        if (!waterRemoved || !spread.isEmpty()) return spread;
        // 原版寻坡可能优先选中水格；该方向不可进入时仍允许其他合法方向流动。
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos target = pos.relative(direction);
            BlockState targetState = level.getBlockState(target);
            FluidState flowing = getNewLiquid(level, target, targetState);
            if (!flowing.isEmpty() && canSpreadTo(level, pos, state, direction, target, targetState,
                    targetState.getFluidState(), flowing.getType())) {
                spread.put(direction, flowing);
            }
        }
        return spread;
    }

    public static final class Source extends OilFluid {
        public Source(Properties properties) {
            super(properties);
        }

        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }

    public static final class Flowing extends OilFluid {
        public Flowing(Properties properties) {
            super(properties);
            registerDefaultState(getStateDefinition().any().setValue(LEVEL, 7));
        }

        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }
}
