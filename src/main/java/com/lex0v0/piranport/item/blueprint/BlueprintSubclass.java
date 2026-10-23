package com.lex0v0.piranport.item.blueprint;

import com.lex0v0.piranport.PiranPort;
import java.util.Arrays;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

// 定义图纸子类、许可归属和对应显示名称。
public enum BlueprintSubclass {
    LARGE(BlueprintFamily.SHIP, "large"),
    MEDIUM(BlueprintFamily.SHIP, "medium"),
    SMALL(BlueprintFamily.SHIP, "small"),
    WEAPON(BlueprintFamily.EQUIPMENT, "weapon"),
    DEVICE(BlueprintFamily.EQUIPMENT, "device"),
    AIRCRAFT(BlueprintFamily.EQUIPMENT, "aircraft");

    private final BlueprintFamily family;
    private final String path;

    BlueprintSubclass(BlueprintFamily family, String path) {
        this.family = family;
        this.path = path;
    }

    public BlueprintFamily family() { return family; }
    public ResourceLocation id() { return ResourceLocation.fromNamespaceAndPath(PiranPort.MODID, family.path() + "/" + path); }
    public String translationKey() { return "blueprint_subclass.piranport." + family.path() + "." + path; }
    public String itemNameKey() { return "item.piranport." + family.path() + "_blueprint." + path; }
    public List<BlueprintType> types() {
        return Arrays.stream(BlueprintType.values()).filter(type -> type.subclass() == this).toList();
    }
}
