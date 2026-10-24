package com.lex0v0.piranport.domain.ship;

import com.lex0v0.piranport.PiranPort;
import java.util.Arrays;
import net.minecraft.resources.ResourceLocation;

// 共用船型与核心容量，稳定 ID 兼容已有蓝图。
public enum ShipSize {
    LARGE("large", 6, 4, 9, 45, 128000),
    MEDIUM("medium", 5, 3, 8, 36, 96000),
    SMALL("small", 4, 2, 6, 27, 64000);

    private final String path;
    private final int weapons, equipment, ammunition, cargo, fuel;

    ShipSize(String path, int weapons, int equipment, int ammunition, int cargo, int fuel) {
        this.path = path;
        this.weapons = weapons;
        this.equipment = equipment;
        this.ammunition = ammunition;
        this.cargo = cargo;
        this.fuel = fuel;
    }

    public String path() { return path; }
    public ResourceLocation id() { return ResourceLocation.fromNamespaceAndPath(PiranPort.MODID, "ship/" + path); }
    public String translationKey() { return "ship_size.piranport." + path; }
    public int weapons() { return weapons; }
    public int equipment() { return equipment; }
    public int ammunition() { return ammunition; }
    public int cargo() { return cargo; }
    public int fuel() { return fuel; }
    public static ShipSize byId(ResourceLocation id) {
        return Arrays.stream(values()).filter(size -> size.id().equals(id)).findFirst().orElse(null);
    }
}
