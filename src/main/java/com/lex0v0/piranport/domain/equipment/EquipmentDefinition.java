package com.lex0v0.piranport.domain.equipment;

import com.lex0v0.piranport.domain.ship.ShipStats;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

// 不可变型号目录；单件损耗和燃油由物品组件保存。
public record EquipmentDefinition(ResourceLocation id, EquipmentCategory category,
        List<String> searchNames, int maxDurability, ShipStats modifiers, int fuelCapacity,
        Optional<GunClass> gunClass, Optional<TorpedoUse> torpedoUse, Optional<ArmorClass> armorClass) {
    public EquipmentDefinition {
        Objects.requireNonNull(id); Objects.requireNonNull(category); Objects.requireNonNull(modifiers);
        searchNames = List.copyOf(searchNames).stream().distinct().toList();
        Objects.requireNonNull(gunClass); Objects.requireNonNull(torpedoUse); Objects.requireNonNull(armorClass);
        if (maxDurability <= 0 || fuelCapacity < 0
                || (category.group() == EquipmentCategory.Group.AIRCRAFT) != (fuelCapacity > 0)
                || (category == EquipmentCategory.MAIN_GUN) != gunClass.isPresent()
                || (category == EquipmentCategory.TORPEDO_LAUNCHER) != torpedoUse.isPresent()
                || (category == EquipmentCategory.ARMOR) != armorClass.isPresent()) throw new IllegalArgumentException("Invalid equipment definition: " + id);
    }
    public String translationKey() { return "equipment.piranport." + id.getPath(); }
    public ResourceLocation texture() { return id.withPath("item/equipment/" + category.path() + "/" + id.getPath()); }
}
