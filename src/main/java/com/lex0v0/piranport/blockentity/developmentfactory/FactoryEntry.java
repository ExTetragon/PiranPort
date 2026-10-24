package com.lex0v0.piranport.blockentity.developmentfactory;

import com.lex0v0.piranport.domain.equipment.EquipmentCategory;
import com.lex0v0.piranport.item.equipment.EquipmentRules;
import com.lex0v0.piranport.registry.ModItems;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

// 浏览元数据将具体耐久型号与普通消耗品放入同一目录，不给消耗品补装备属性。
public record FactoryEntry(ResourceLocation id, EquipmentCategory category, List<String> searchNames, boolean model) {
    public FactoryEntry { searchNames = List.copyOf(searchNames); }
    public String translationKey() { return (model ? "equipment.piranport." : "item.piranport.") + id.getPath(); }
    public int maxStack() { return model ? 1 : category == EquipmentCategory.QUICK_REPAIR ? 64 : 16; }
    public ItemStack create(int count) {
        if(count<1 || count>maxStack()) throw new IllegalArgumentException("Invalid development output count");
        ItemStack stack = model ? EquipmentRules.create(id) : new ItemStack(ModItems.equipment(category));
        stack.setCount(count); return stack;
    }
}
