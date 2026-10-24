package com.lex0v0.piranport.blockentity.developmentfactory;

import com.lex0v0.piranport.domain.equipment.EquipmentCategory;
import com.lex0v0.piranport.text.SearchText;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

// 类别取并集，再与无大小写及全半角差别的文字条件取交集。
public record FactoryFilter(String text, Set<EquipmentCategory> categories) {
    public static final int MAX_TEXT_LENGTH=128;
    public static final FactoryFilter NONE=new FactoryFilter("",Set.of());
    public FactoryFilter { text=text.strip(); if(text.length()>MAX_TEXT_LENGTH)text=text.substring(0,MAX_TEXT_LENGTH); categories=categories.size()==EquipmentCategory.values().length?Set.of():Set.copyOf(categories); }
    public boolean active(){return !text.isEmpty()||!categories.isEmpty();}
    public boolean matches(FactoryEntry entry){return(categories.isEmpty()||categories.contains(entry.category()))&&SearchText.matches(text,entry.searchNames());}
    public CompoundTag save(){CompoundTag tag=new CompoundTag();tag.putString("Text",text);ListTag list=new ListTag();for(var c:EquipmentCategory.values())if(categories.contains(c))list.add(StringTag.valueOf(c.path()));tag.put("Categories",list);return tag;}
    public static FactoryFilter load(CompoundTag tag){var categories=java.util.EnumSet.noneOf(EquipmentCategory.class);for(Tag value:tag.getList("Categories",Tag.TAG_STRING))for(var c:EquipmentCategory.values())if(c.path().equals(value.getAsString()))categories.add(c);return new FactoryFilter(tag.getString("Text"),categories);}
}
