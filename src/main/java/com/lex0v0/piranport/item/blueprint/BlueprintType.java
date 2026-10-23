package com.lex0v0.piranport.item.blueprint;

import com.lex0v0.piranport.PiranPort;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;

// 以稳定 ID 定义具体许可，独立于译名和枚举顺序。
public enum BlueprintType {
    BATTLESHIP(BlueprintSubclass.LARGE, "battleship"),
    BATTLECRUISER(BlueprintSubclass.LARGE, "battlecruiser"),
    AIRCRAFT_CARRIER(BlueprintSubclass.LARGE, "aircraft_carrier"),
    HEAVY_CRUISER(BlueprintSubclass.MEDIUM, "heavy_cruiser"),
    LIGHT_AIRCRAFT_CARRIER(BlueprintSubclass.MEDIUM, "light_aircraft_carrier"),
    LIGHT_CRUISER(BlueprintSubclass.MEDIUM, "light_cruiser"),
    MONITOR(BlueprintSubclass.SMALL, "monitor"),
    DESTROYER(BlueprintSubclass.SMALL, "destroyer"),
    SUBMARINE(BlueprintSubclass.SMALL, "submarine"),
    MAIN_GUN(BlueprintSubclass.WEAPON, "main_gun"),
    TORPEDO_LAUNCHER(BlueprintSubclass.WEAPON, "torpedo_launcher"),
    DEPTH_CHARGE_PROJECTOR(BlueprintSubclass.WEAPON, "depth_charge_projector"),
    MISSILE_LAUNCHER(BlueprintSubclass.WEAPON, "missile_launcher"),
    ARMOR(BlueprintSubclass.DEVICE, "armor"),
    PROPULSION_SYSTEM(BlueprintSubclass.DEVICE, "propulsion_system"),
    RADAR(BlueprintSubclass.DEVICE, "radar"),
    SONAR(BlueprintSubclass.DEVICE, "sonar"),
    QUICK_REPAIR(BlueprintSubclass.DEVICE, "quick_repair"),
    DAMAGE_CONTROL(BlueprintSubclass.DEVICE, "damage_control"),
    FIGHTER(BlueprintSubclass.AIRCRAFT, "fighter"),
    BOMBER(BlueprintSubclass.AIRCRAFT, "bomber"),
    TORPEDO_BOMBER(BlueprintSubclass.AIRCRAFT, "torpedo_bomber"),
    RECONNAISSANCE_AIRCRAFT(BlueprintSubclass.AIRCRAFT, "reconnaissance_aircraft");

    private static final Map<ResourceLocation, BlueprintType> BY_ID = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(BlueprintType::id, Function.identity()));
    private final BlueprintSubclass subclass;
    private final String path;

    BlueprintType(BlueprintSubclass subclass, String path) {
        this.subclass = subclass;
        this.path = path;
    }

    public BlueprintSubclass subclass() { return subclass; }
    public BlueprintFamily family() { return subclass.family(); }
    public ResourceLocation id() { return ResourceLocation.fromNamespaceAndPath(PiranPort.MODID, family().path() + "/" + path); }
    public String translationKey() { return "blueprint_type.piranport." + family().path() + "." + path; }
    public static BlueprintType byId(ResourceLocation id) { return BY_ID.get(id); }
}
