package com.lex0v0.piranport.item.equipment;

import com.lex0v0.piranport.logging.ModLog;
import com.lex0v0.piranport.domain.equipment.EquipmentCategory;
import com.lex0v0.piranport.item.EquipmentItem;
import com.lex0v0.piranport.registry.ModDataComponents;
import com.lex0v0.piranport.registry.ModFluids;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;

// 飞机储油与核心复用不可变流体组件；异常身份和超额流体仍可抽取。
public final class AircraftFluidHandler extends FluidHandlerItemStack {
    public AircraftFluidHandler(ItemStack stack) { super(ModDataComponents.FLUID_CONTENT, stack, 0); }
    private void checkTank(int tank) { if (tank != 0) throw new IllegalArgumentException("Tank out of range"); }
    @Override public FluidStack getFluid() {
        return !container.isEmpty() && container.getItem() instanceof EquipmentItem item && item.category().group() == EquipmentCategory.Group.AIRCRAFT
                ? super.getFluid() : FluidStack.EMPTY;
    }
    @Override public FluidStack getFluidInTank(int tank) { checkTank(tank); return getFluid(); }
    @Override public int getTankCapacity(int tank) { checkTank(tank); return EquipmentRules.fuelCapacity(container); }
    @Override public boolean isFluidValid(int tank, FluidStack fluid) { checkTank(tank); return canFillFluidType(fluid); }
    @Override public boolean canFillFluidType(FluidStack fluid) {
        return EquipmentRules.definition(container).filter(value -> value.category().group() == EquipmentCategory.Group.AIRCRAFT).isPresent()
                && fluid.is(ModFluids.FUEL_OIL.get());
    }
    @Override public int fill(FluidStack fluid, FluidAction action) {
        capacity = getTankCapacity(0);
        int filled = getFluid().getAmount() >= capacity ? 0 : super.fill(fluid, action);
        if (action.execute() && filled > 0) ModLog.aggregate(ModLog.Event.AIRCRAFT_FILL, filled, true);
        return filled;
    }
    @Override public FluidStack drain(int amount, FluidAction action) {
        FluidStack drained = super.drain(amount, action);
        if (action.execute() && !drained.isEmpty()) ModLog.aggregate(ModLog.Event.AIRCRAFT_DRAIN, drained.getAmount(), true);
        return drained;
    }
}
