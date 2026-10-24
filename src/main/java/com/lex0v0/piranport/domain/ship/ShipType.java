package com.lex0v0.piranport.domain.ship;

import com.lex0v0.piranport.PiranPort;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;

// 核心与蓝图共用的舰种、船型关系和译名入口。
public enum ShipType {
    BATTLESHIP("battleship", ShipSize.LARGE),
    BATTLECRUISER("battlecruiser", ShipSize.LARGE),
    AIRCRAFT_CARRIER("aircraft_carrier", ShipSize.LARGE),
    HEAVY_CRUISER("heavy_cruiser", ShipSize.MEDIUM),
    LIGHT_AIRCRAFT_CARRIER("light_aircraft_carrier", ShipSize.MEDIUM),
    LIGHT_CRUISER("light_cruiser", ShipSize.MEDIUM),
    MONITOR("monitor", ShipSize.SMALL),
    DESTROYER("destroyer", ShipSize.SMALL),
    SUBMARINE("submarine", ShipSize.SMALL);

    private static final Map<ResourceLocation, ShipType> BY_ID = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(ShipType::id, Function.identity()));
    private final String path;
    private final ShipSize size;

    ShipType(String path, ShipSize size) { this.path = path; this.size = size; }
    public String path() { return path; }
    public ShipSize size() { return size; }
    public ResourceLocation id() { return ResourceLocation.fromNamespaceAndPath(PiranPort.MODID, "ship/" + path); }
    public String translationKey() { return "ship_type.piranport." + path; }
    public static ShipType byId(ResourceLocation id) { return BY_ID.get(id); }
}
