package com.lex0v0.piranport.blockentity.developmentfactory;

import com.lex0v0.piranport.domain.equipment.EquipmentCategory;
import com.lex0v0.piranport.domain.equipment.EquipmentDefinitions;
import com.lex0v0.piranport.item.equipment.EquipmentRules;
import com.lex0v0.piranport.registry.ModItems;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

// 稳定目录只引用型号与物品身份，创建物品时才读取注册项。
public final class FactoryCatalog {
    private FactoryCatalog() {}
    public static final List<FactoryEntry> ALL = Stream.concat(EquipmentDefinitions.ALL.stream()
            .map(d -> new FactoryEntry(d.id(),d.category(),d.searchNames(),true)), Stream.of(
            new FactoryEntry(ResourceLocation.parse("piranport:quick_repair"),EquipmentCategory.QUICK_REPAIR,List.of("快速修理工具组","快速修理工具","修理工具组","快修","Quick Repair Tool Set","Quick Repair"),false),
            new FactoryEntry(ResourceLocation.parse("piranport:damage_control"),EquipmentCategory.DAMAGE_CONTROL,List.of("损害管制工具组","损害管制","损害管制小组","损管","Damage Control Tool Set","Damage Control"),false)))
            .sorted(java.util.Comparator.comparingInt(e -> e.category().ordinal())).toList();
    public static FactoryEntry byId(ResourceLocation id) { return ALL.stream().filter(e -> e.id().equals(id)).findFirst().orElse(null); }
    public static Optional<FactoryEntry> definition(ItemStack stack) {
        if(stack.isEmpty())return Optional.empty();
        var model=EquipmentRules.definition(stack);
        if(model.isPresent())return Optional.ofNullable(byId(model.get().id()));
        if(stack.is(ModItems.QUICK_REPAIR.get()))return Optional.ofNullable(byId(ResourceLocation.parse("piranport:quick_repair")));
        if(stack.is(ModItems.DAMAGE_CONTROL.get()))return Optional.ofNullable(byId(ResourceLocation.parse("piranport:damage_control")));
        return Optional.empty();
    }
}
