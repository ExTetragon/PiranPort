package com.lex0v0.piranport.item;

import com.lex0v0.piranport.Config;
import com.lex0v0.piranport.logging.ModLog;
import com.lex0v0.piranport.registry.ModDataComponents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;

// 携带的燃料桶按单格配置转移流体，降容保留余量并允许抽出。
public final class FuelBarrelItemFluidHandler extends FluidHandlerItemStack {
    public FuelBarrelItemFluidHandler(ItemStack stack) { super(ModDataComponents.FLUID_CONTENT, stack, Config.FUEL_BARREL_CAPACITY.get()); }
    @Override public int getTankCapacity(int index) {
        if (index != 0) throw new IllegalArgumentException("Tank out of range");
        return Config.FUEL_BARREL_CAPACITY.get();
    }
    @Override public int fill(FluidStack fluid, FluidAction action) {
        capacity = getTankCapacity(0);
        int filled = getFluid().getAmount() >= capacity ? 0 : super.fill(fluid, action);
        if (action.execute() && filled > 0) ModLog.aggregate(ModLog.Event.BARREL_ITEM_FILL, filled, true);
        return filled;
    }
    @Override public FluidStack drain(int amount, FluidAction action) {
        FluidStack drained = super.drain(amount, action);
        if (action.execute() && !drained.isEmpty()) ModLog.aggregate(ModLog.Event.BARREL_ITEM_DRAIN, drained.getAmount(), true);
        return drained;
    }
}
