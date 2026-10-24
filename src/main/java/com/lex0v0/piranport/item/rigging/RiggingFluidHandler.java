package com.lex0v0.piranport.item.rigging;

import com.lex0v0.piranport.registry.ModDataComponents;
import com.lex0v0.piranport.registry.ModFluids;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;

// 只灌入燃油，动态读取容量，异常身份和降容超额流体仍能抽取。
public final class RiggingFluidHandler extends FluidHandlerItemStack {
    public RiggingFluidHandler(ItemStack core) { super(ModDataComponents.FLUID_CONTENT, core, 0); }
    private void checkTank(int tank) { if (tank != 0) throw new IllegalArgumentException("Tank out of range"); }
    @Override public FluidStack getFluid() { return RiggingCoreRules.isCore(container) ? super.getFluid() : FluidStack.EMPTY; }
    @Override public FluidStack getFluidInTank(int tank) { checkTank(tank); return getFluid(); }
    @Override public int getTankCapacity(int tank) { checkTank(tank); return RiggingCoreRules.fuelCapacity(container); }
    @Override public boolean isFluidValid(int tank, FluidStack fluid) { checkTank(tank); return canFillFluidType(fluid); }
    @Override public boolean canFillFluidType(FluidStack fluid) {
        return RiggingCoreRules.status(container) == RiggingCoreRules.IdentityStatus.VALID && fluid.is(ModFluids.FUEL_OIL.get());
    }
    @Override public int fill(FluidStack fluid, FluidAction action) {
        capacity = getTankCapacity(0);
        if (getFluid().getAmount() >= capacity) return 0;
        return super.fill(fluid, action);
    }
}
