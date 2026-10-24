package com.lex0v0.piranport.blockentity.shipyard;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

// 每个打开窗口独立浏览，最后关闭者写回机器。
public final class ShipyardBrowseState {
    public boolean all;
    public int scroll;
    public ResourceLocation selected;
    public ShipyardFilter filter = ShipyardFilter.NONE;

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("All", all); tag.putInt("Scroll", scroll);
        if (selected != null) tag.putString("Selected", selected.toString());
        tag.put("Filter", filter.save());
        return tag;
    }

    public static ShipyardBrowseState load(CompoundTag tag) {
        ShipyardBrowseState state = new ShipyardBrowseState();
        state.all = tag.getBoolean("All"); state.scroll = Math.max(0, tag.getInt("Scroll"));
        state.selected = ResourceLocation.tryParse(tag.getString("Selected"));
        state.filter = ShipyardFilter.load(tag.getCompound("Filter"));
        return state;
    }

    public ShipyardBrowseState copy() { return load(save()); }
}
