package com.lex0v0.piranport.item.rigging;

import net.minecraft.world.item.ItemStack;
import com.lex0v0.piranport.registry.ModItemTags;
import net.neoforged.neoforge.items.ComponentItemHandler;

// 动态解析容量，超出当前容量的槽位禁止新增但允许取出。
public final class RiggingItemHandler extends ComponentItemHandler {
    private final ItemStack core;
    private final RiggingRegion region;

    public RiggingItemHandler(ItemStack core, RiggingRegion region) {
        super(core, region.component(), 0);
        this.core = core;
        this.region = region;
    }
    @Override
    public int getSlots() {
        return RiggingCoreRules.isCore(core) ? Math.max(RiggingCoreRules.capacity(core, region), getContents().getSlots()) : 0;
    }
    @Override
    public int getSlotLimit(int slot) { validateSlotIndex(slot); return region == RiggingRegion.WEAPONS ? 1 : region == RiggingRegion.EQUIPMENT ? 64 : super.getSlotLimit(slot); }
    private int limit(int slot, ItemStack stack) {
        return Math.min(getSlotLimit(slot), region == RiggingRegion.EQUIPMENT && !stack.is(ModItemTags.EQUIPMENT_CONSUMABLES) ? 1 : stack.getMaxStackSize());
    }
    @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        validateSlotIndex(slot);
        if(stack.isEmpty() || !isItemValid(slot,stack))return stack;
        var contents=getContents();var existing=getStackFromContents(contents,slot);
        if(!existing.isEmpty()&&!ItemStack.isSameItemSameComponents(existing,stack))return stack;
        int count=Math.min(stack.getCount(),Math.max(0,limit(slot,stack)-existing.getCount()));
        if(count>0&&!simulate)updateContents(contents,stack.copyWithCount(existing.getCount()+count),slot);
        return stack.copyWithCount(stack.getCount()-count);
    }
    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        validateSlotIndex(slot);
        return stack.isEmpty() || slot < RiggingCoreRules.capacity(core, region) && RiggingSlotRules.accepts(region, stack);
    }
    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        validateSlotIndex(slot);
        if (!stack.isEmpty() && (!isItemValid(slot, stack) || stack.getCount() > limit(slot,stack)))
            throw new IllegalArgumentException("Invalid rigging stack or count");
        super.setStackInSlot(slot, stack);
    }
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        validateSlotIndex(slot);
        return amount <= 0 ? ItemStack.EMPTY : super.extractItem(slot, amount, simulate);
    }
    public int occupiedSlots() {
        return (int)getContents().nonEmptyStream().count();
    }
}
