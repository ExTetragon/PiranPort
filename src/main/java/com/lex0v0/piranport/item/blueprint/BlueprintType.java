package com.lex0v0.piranport.item.blueprint;

import com.lex0v0.piranport.PiranPort;
import com.lex0v0.piranport.domain.ship.ShipType;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;

// 以稳定 ID 定义具体许可，独立于译名和枚举顺序。
public enum BlueprintType {
    BATTLESHIP(ShipType.BATTLESHIP),
    BATTLECRUISER(ShipType.BATTLECRUISER),
    AIRCRAFT_CARRIER(ShipType.AIRCRAFT_CARRIER),
    HEAVY_CRUISER(ShipType.HEAVY_CRUISER),
    LIGHT_AIRCRAFT_CARRIER(ShipType.LIGHT_AIRCRAFT_CARRIER),
    LIGHT_CRUISER(ShipType.LIGHT_CRUISER),
    MONITOR(ShipType.MONITOR),
    DESTROYER(ShipType.DESTROYER),
    SUBMARINE(ShipType.SUBMARINE),
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
    SECONDARY_GUN(BlueprintSubclass.DEVICE, "secondary_gun"),
    FIGHTER(BlueprintSubclass.AIRCRAFT, "fighter"),
    BOMBER(BlueprintSubclass.AIRCRAFT, "bomber"),
    TORPEDO_BOMBER(BlueprintSubclass.AIRCRAFT, "torpedo_bomber"),
    RECONNAISSANCE_AIRCRAFT(BlueprintSubclass.AIRCRAFT, "reconnaissance_aircraft");

    private static final Map<ResourceLocation, BlueprintType> BY_ID = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(BlueprintType::id, Function.identity()));
    private final BlueprintSubclass subclass;
    private final String path;
    private final ShipType shipType;

    BlueprintType(BlueprintSubclass subclass, String path) {
        this.subclass = subclass;
        this.path = path;
        this.shipType = null;
    }

    BlueprintType(ShipType type) {
        this.subclass = BlueprintSubclass.forShipSize(type.size());
        this.path = type.path();
        this.shipType = type;
    }

    public BlueprintSubclass subclass() { return subclass; }
    public BlueprintFamily family() { return subclass.family(); }
    public ResourceLocation id() { return shipType != null ? shipType.id() : ResourceLocation.fromNamespaceAndPath(PiranPort.MODID, family().path() + "/" + path); }
    public String translationKey() { return shipType != null ? shipType.translationKey() : "blueprint_type.piranport." + family().path() + "." + path; }
    public ShipType shipType() { return shipType; }
    public static BlueprintType byId(ResourceLocation id) { return BY_ID.get(id); }
}
