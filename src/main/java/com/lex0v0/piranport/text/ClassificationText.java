package com.lex0v0.piranport.text;

import com.lex0v0.piranport.domain.equipment.EquipmentCategory;
import com.lex0v0.piranport.domain.equipment.EquipmentDefinition;
import com.lex0v0.piranport.domain.ship.ShipDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

// 界面和物品提示共用分类文字，标点与间距由语言资源决定。
public final class ClassificationText {
    private ClassificationText() {}

    public static MutableComponent ship(ShipDefinition ship) {
        return Component.translatable("classification.piranport.ship",
                Component.translatable(ship.size().translationKey()),
                Component.translatable(ship.type().translationKey()),
                Component.translatable(ship.nationality().translationKey()));
    }

    public static MutableComponent equipment(EquipmentCategory category) {
        return Component.translatable(category.translationKey());
    }

    public static MutableComponent equipment(EquipmentDefinition definition) {
        var subtype = definition.gunClass().map(type -> type.translationKey())
                .or(() -> definition.torpedoUse().map(type -> type.translationKey()))
                .or(() -> definition.armorClass().map(type -> type.translationKey()));
        return subtype.map(key -> Component.translatable("classification.piranport.equipment",
                equipment(definition.category()), Component.translatable(key)))
                .orElseGet(() -> equipment(definition.category()));
    }
}
