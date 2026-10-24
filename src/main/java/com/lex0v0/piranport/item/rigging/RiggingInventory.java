package com.lex0v0.piranport.item.rigging;

import java.util.Arrays;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

// 统一物品能力视图，分区偏移每次查询，存档仍由四个组件独立维护。
public final class RiggingInventory implements IItemHandlerModifiable {
    private final List<RiggingItemHandler> regions;
    public RiggingInventory(ItemStack core) {
        regions = Arrays.stream(RiggingRegion.values()).map(region -> new RiggingItemHandler(core, region)).toList();
    }
    @Override public int getSlots() { return regions.stream().mapToInt(RiggingItemHandler::getSlots).sum(); }
    private record Slot(RiggingItemHandler handler, int index) {}
    private Slot slot(int index) {
        if (index < 0) throw new IllegalArgumentException("Negative slot");
        for (var region : regions) {
            if (index < region.getSlots()) return new Slot(region, index);
            index -= region.getSlots();
        }
        throw new IllegalArgumentException("Slot out of range");
    }
    @Override public ItemStack getStackInSlot(int slot) { var s = slot(slot); return s.handler().getStackInSlot(s.index()); }
    @Override public void setStackInSlot(int slot, ItemStack stack) { var s = slot(slot); s.handler().setStackInSlot(s.index(), stack); }
    @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { var s = slot(slot); return s.handler().insertItem(s.index(), stack, simulate); }
    @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { var s = slot(slot); return s.handler().extractItem(s.index(), amount, simulate); }
    @Override public int getSlotLimit(int slot) { var s = slot(slot); return s.handler().getSlotLimit(s.index()); }
    @Override public boolean isItemValid(int slot, ItemStack stack) { var s = slot(slot); return s.handler().isItemValid(s.index(), stack); }
}
