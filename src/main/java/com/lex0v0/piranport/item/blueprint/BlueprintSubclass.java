package com.lex0v0.piranport.item.blueprint;

import com.lex0v0.piranport.PiranPort;
import com.lex0v0.piranport.domain.ship.ShipSize;
import java.util.Arrays;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

// 定义图纸子类、许可归属和对应显示名称。
public enum BlueprintSubclass {
    LARGE(ShipSize.LARGE),
    MEDIUM(ShipSize.MEDIUM),
    SMALL(ShipSize.SMALL),
    WEAPON(BlueprintFamily.EQUIPMENT, "weapon"),
    DEVICE(BlueprintFamily.EQUIPMENT, "device"),
    AIRCRAFT(BlueprintFamily.EQUIPMENT, "aircraft");

    private final BlueprintFamily family;
    private final String path;
    private final ShipSize shipSize;

    BlueprintSubclass(BlueprintFamily family, String path) {
        this.family = family;
        this.path = path;
        this.shipSize = null;
    }

    BlueprintSubclass(ShipSize size) {
        this.family = BlueprintFamily.SHIP;
        this.path = size.path();
        this.shipSize = size;
    }

    public BlueprintFamily family() { return family; }
    public ResourceLocation id() { return shipSize != null ? shipSize.id() : ResourceLocation.fromNamespaceAndPath(PiranPort.MODID, family.path() + "/" + path); }
    public String translationKey() { return shipSize != null ? shipSize.translationKey() : "blueprint_subclass.piranport." + family.path() + "." + path; }
    public static BlueprintSubclass forShipSize(ShipSize size) {
        return Arrays.stream(values()).filter(subclass -> subclass.shipSize == size).findFirst().orElseThrow();
    }
    public String itemNameKey() { return "item.piranport." + family.path() + "_blueprint." + path; }
    public List<BlueprintType> types() {
        return Arrays.stream(BlueprintType.values()).filter(type -> type.subclass() == this).toList();
    }
}
