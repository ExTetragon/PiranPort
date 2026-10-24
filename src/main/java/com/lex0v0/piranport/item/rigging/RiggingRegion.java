package com.lex0v0.piranport.item.rigging;

import com.lex0v0.piranport.registry.ModDataComponents;
import com.lex0v0.piranport.domain.ship.ShipSize;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.component.ItemContainerContents;

// 四个独立组件对应四类空间，统一视图不成为存档结构。
public enum RiggingRegion {
    WEAPONS, EQUIPMENT, AMMUNITION, CARGO;

    public DataComponentType<ItemContainerContents> component() {
        return switch (this) {
            case WEAPONS -> ModDataComponents.RIGGING_WEAPONS.get();
            case EQUIPMENT -> ModDataComponents.RIGGING_EQUIPMENT.get();
            case AMMUNITION -> ModDataComponents.RIGGING_AMMUNITION.get();
            case CARGO -> ModDataComponents.RIGGING_CARGO.get();
        };
    }
    public int capacity(ShipSize size) {
        return switch (this) {
            case WEAPONS -> size.weapons();
            case EQUIPMENT -> size.equipment();
            case AMMUNITION -> size.ammunition();
            case CARGO -> size.cargo();
        };
    }
    public boolean singleItem() { return this == WEAPONS || this == EQUIPMENT; }
    public String translationKey() { return "tooltip.piranport.rigging." + name().toLowerCase(java.util.Locale.ROOT); }
}
