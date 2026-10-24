package com.lex0v0.piranport.blockentity.developmentfactory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

// 每个打开窗口独立浏览，最后关闭者写回机器。
public final class FactoryBrowseState {
    public boolean all;
    public int scroll;
    public ResourceLocation selected;
    public FactoryFilter filter = FactoryFilter.NONE;

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("All", all); tag.putInt("Scroll", scroll);
        if (selected != null) tag.putString("Selected", selected.toString());
        tag.put("Filter", filter.save());
        return tag;
    }

    public static FactoryBrowseState load(CompoundTag tag) {
        FactoryBrowseState state = new FactoryBrowseState();
        state.all = tag.getBoolean("All"); state.scroll = Math.max(0, tag.getInt("Scroll"));
        state.selected = ResourceLocation.tryParse(tag.getString("Selected"));
        state.filter = FactoryFilter.load(tag.getCompound("Filter"));
        return state;
    }

    public FactoryBrowseState copy() { return load(save()); }
}
